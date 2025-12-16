package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.domain.Stocktake;
import com.example.crp.inventory.domain.StocktakeLine;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentSpecifications;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.repo.StocktakeLineRepository;
import com.example.crp.inventory.repo.StocktakeRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class StocktakeService {

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeLineRepository lineRepository;
    private final EquipmentRepository equipmentRepository;
    private final LocationRepository locationRepository;
    private final EquipmentLifecycleService lifecycleService;
    private final LocationAccessPolicy locationAccessPolicy;
    private final OutboxService outboxService;

    public StocktakeService(StocktakeRepository stocktakeRepository,
                            StocktakeLineRepository lineRepository,
                            EquipmentRepository equipmentRepository,
                            LocationRepository locationRepository,
                            EquipmentLifecycleService lifecycleService,
                            LocationAccessPolicy locationAccessPolicy,
                            OutboxService outboxService) {
        this.stocktakeRepository = stocktakeRepository;
        this.lineRepository = lineRepository;
        this.equipmentRepository = equipmentRepository;
        this.locationRepository = locationRepository;
        this.lifecycleService = lifecycleService;
        this.locationAccessPolicy = locationAccessPolicy;
        this.outboxService = outboxService;
    }

    public List<Stocktake> listByLocation(Long locationId) {
        return stocktakeRepository.findTop100ByLocationIdOrderByCreatedAtDesc(locationId);
    }

    public Stocktake get(Long id) {
        return stocktakeRepository.findById(id).orElseThrow();
    }

    public List<StocktakeLine> lines(Long stocktakeId) {
        return lineRepository.findByStocktakeIdOrderByIdAsc(stocktakeId);
    }

    @Transactional
    public Stocktake create(Long locationId,
                            String title,
                            String createdBy,
                            String correlationId,
                            org.springframework.security.core.Authentication auth) {
        if (locationId == null) throw new IllegalArgumentException("locationId is required");
        Location location = locationRepository.findById(locationId).orElseThrow();
        locationAccessPolicy.assertWriteAllowed(auth, location);

        Stocktake st = new Stocktake();
        st.setLocationId(locationId);
        st.setStatus("OPEN");
        st.setTitle(title);
        st.setCreatedBy(createdBy);
        st.setCorrelationId(correlationId);
        Stocktake saved = stocktakeRepository.save(st);

        Specification<Equipment> spec = Specification.where(EquipmentSpecifications.locationIdEquals(locationId));
        List<Equipment> expected = equipmentRepository.findAll(spec);

        List<StocktakeLine> lines = expected.stream().map(e -> {
            StocktakeLine l = new StocktakeLine();
            l.setStocktakeId(saved.getId());
            l.setEquipmentId(e.getId());
            l.setInventoryNumber(e.getInventoryNumber());
            l.setSerialNumber(e.getSerialNumber());
            l.setExpectedLocationId(e.getLocationId());
            l.setExpectedStatus(e.getStatus());
            l.setExpectedResponsible(e.getResponsibleUsername());
            l.setCountedPresent(null);
            return l;
        }).toList();
        if (!lines.isEmpty()) {
            lineRepository.saveAll(lines);
        }

        outboxService.enqueue("Stocktake", saved.getId(), "inventory.stocktake.created",
                "InventoryStocktakeCreated", new Events.InventoryStocktakeCreated(
                        saved.getId(),
                        locationId,
                        title,
                        expected.size(),
                        createdBy,
                        correlationId
                ));

        return saved;
    }

    @Transactional
    public StocktakeLine countLine(Long stocktakeId,
                                   Long equipmentId,
                                   Boolean present,
                                   Long countedLocationId,
                                   String countedStatus,
                                   String note,
                                   String countedBy) {
        Stocktake st = stocktakeRepository.findById(stocktakeId).orElseThrow();
        if (!"OPEN".equals(st.getStatus())) {
            throw new IllegalStateException("Stocktake must be OPEN");
        }
        if (equipmentId == null) throw new IllegalArgumentException("equipmentId is required");

        StocktakeLine line = lineRepository.findByStocktakeIdAndEquipmentId(stocktakeId, equipmentId).orElse(null);
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow();

        if (line == null) {
            // Found item not expected in this location snapshot
            line = new StocktakeLine();
            line.setStocktakeId(stocktakeId);
            line.setEquipmentId(equipmentId);
            line.setInventoryNumber(equipment.getInventoryNumber());
            line.setSerialNumber(equipment.getSerialNumber());
            line.setExpectedLocationId(equipment.getLocationId());
            line.setExpectedStatus(equipment.getStatus());
            line.setExpectedResponsible(equipment.getResponsibleUsername());
        }

        line.setCountedPresent(present);
        line.setCountedLocationId(countedLocationId);
        line.setCountedStatus(countedStatus == null ? null : countedStatus.trim().toUpperCase());
        line.setCountedBy(countedBy);
        line.setCountedAt(OffsetDateTime.now());
        line.setNote(note == null ? null : trim(note, 256));
        return lineRepository.save(line);
    }

    @Transactional
    public Stocktake submit(Long stocktakeId, String submittedBy, String correlationId) {
        Stocktake st = stocktakeRepository.findById(stocktakeId).orElseThrow();
        if (!"OPEN".equals(st.getStatus())) {
            throw new IllegalStateException("Stocktake must be OPEN");
        }
        st.setStatus("SUBMITTED");
        st.setSubmittedAt(OffsetDateTime.now());
        st.setCorrelationId(correlationId);
        Stocktake saved = stocktakeRepository.save(st);

        List<StocktakeLine> lines = lineRepository.findByStocktakeIdOrderByIdAsc(stocktakeId);
        int counted = (int) lines.stream().filter(l -> l.getCountedAt() != null).count();

        outboxService.enqueue("Stocktake", saved.getId(), "inventory.stocktake.submitted",
                "InventoryStocktakeSubmitted", new Events.InventoryStocktakeSubmitted(
                        saved.getId(),
                        saved.getLocationId(),
                        lines.size(),
                        counted,
                        submittedBy,
                        correlationId
                ));
        return saved;
    }

    public record CloseResult(Stocktake stocktake,
                              int missingCount,
                              int movedCount,
                              int statusMismatchCount,
                              boolean applied) {}

    @Transactional
    public CloseResult close(Long stocktakeId,
                             boolean apply,
                             String closedBy,
                             String correlationId,
                             org.springframework.security.core.Authentication auth) {
        Stocktake st = stocktakeRepository.findById(stocktakeId).orElseThrow();
        if (!"SUBMITTED".equals(st.getStatus()) && !"OPEN".equals(st.getStatus())) {
            throw new IllegalStateException("Stocktake must be OPEN or SUBMITTED");
        }
        Location location = locationRepository.findById(st.getLocationId()).orElseThrow();
        locationAccessPolicy.assertWriteAllowed(auth, location);

        List<StocktakeLine> lines = lineRepository.findByStocktakeIdOrderByIdAsc(stocktakeId);

        int missing = 0;
        int moved = 0;
        int statusMismatch = 0;

        for (StocktakeLine l : lines) {
            if (l.getEquipmentId() == null) continue;
            boolean counted = l.getCountedAt() != null;
            if (!counted) continue;

            Boolean present = l.getCountedPresent();
            if (present != null && !present) {
                missing++;
                continue;
            }

            if (l.getCountedLocationId() != null && l.getExpectedLocationId() != null && !l.getCountedLocationId().equals(l.getExpectedLocationId())) {
                moved++;
            }
            if (l.getCountedStatus() != null && l.getExpectedStatus() != null && !l.getCountedStatus().equalsIgnoreCase(l.getExpectedStatus())) {
                statusMismatch++;
            }

            if (apply) {
                if (l.getCountedLocationId() != null && (l.getExpectedLocationId() == null || !l.getCountedLocationId().equals(l.getExpectedLocationId()))) {
                    lifecycleService.transfer(l.getEquipmentId(), l.getCountedLocationId(), null, "stocktake", closedBy, correlationId, auth);
                }
                if (l.getCountedStatus() != null && (l.getExpectedStatus() == null || !l.getCountedStatus().equalsIgnoreCase(l.getExpectedStatus()))) {
                    lifecycleService.changeStatus(l.getEquipmentId(), l.getCountedStatus(), "stocktake", closedBy, correlationId, auth);
                }
            }
        }

        st.setStatus("CLOSED");
        st.setClosedAt(OffsetDateTime.now());
        st.setCorrelationId(correlationId);
        Stocktake saved = stocktakeRepository.save(st);

        outboxService.enqueue("Stocktake", saved.getId(), "inventory.stocktake.closed",
                "InventoryStocktakeClosed", new Events.InventoryStocktakeClosed(
                        saved.getId(),
                        saved.getLocationId(),
                        missing,
                        moved,
                        statusMismatch,
                        closedBy,
                        apply,
                        correlationId
                ));

        return new CloseResult(saved, missing, moved, statusMismatch, apply);
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.length() > max) return t.substring(0, max);
        return t;
    }
}
