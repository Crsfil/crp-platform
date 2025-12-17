package com.example.crp.procurement.service;

import com.example.crp.procurement.domain.ProcurementRfq;
import com.example.crp.procurement.domain.ProcurementRfqOffer;
import com.example.crp.procurement.domain.ProcurementServiceOrder;
import com.example.crp.procurement.messaging.Events;
import com.example.crp.procurement.outbox.OutboxService;
import com.example.crp.procurement.repo.ProcurementRfqOfferRepository;
import com.example.crp.procurement.repo.ProcurementRfqRepository;
import com.example.crp.procurement.repo.ProcurementRequestRepository;
import com.example.crp.procurement.repo.ProcurementServiceOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProcurementServiceOrderService {

    private static final java.util.Set<String> SERVICE_TYPES = java.util.Set.of(
            "SERVICE_STORAGE", "SERVICE_VALUATION", "SERVICE_REPAIR", "SERVICE_AUCTION"
    );
    private static final java.util.Set<String> COMPLETABLE_STATUSES = java.util.Set.of(
            "CREATED", "IN_PROGRESS"
    );

    private final ProcurementServiceOrderRepository serviceOrderRepository;
    private final ProcurementRfqRepository rfqRepository;
    private final ProcurementRfqOfferRepository rfqOfferRepository;
    private final ProcurementRequestRepository requestRepository;
    private final OutboxService outboxService;

    public ProcurementServiceOrderService(ProcurementServiceOrderRepository serviceOrderRepository,
                                          ProcurementRfqRepository rfqRepository,
                                          ProcurementRfqOfferRepository rfqOfferRepository,
                                          ProcurementRequestRepository requestRepository,
                                          OutboxService outboxService) {
        this.serviceOrderRepository = serviceOrderRepository;
        this.rfqRepository = rfqRepository;
        this.rfqOfferRepository = rfqOfferRepository;
        this.requestRepository = requestRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public ProcurementServiceOrder create(ProcurementServiceOrder order) {
        String serviceType = normalize(order.getServiceType());
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is required");
        }
        if (!SERVICE_TYPES.contains(serviceType)) {
            throw new IllegalArgumentException("Unsupported serviceType: " + serviceType);
        }
        order.setServiceType(serviceType);
        if (order.getRequestId() != null && requestRepository.findById(order.getRequestId()).isEmpty()) {
            throw new IllegalArgumentException("request not found");
        }
        order.setStatus("CREATED");
        ProcurementServiceOrder saved = serviceOrderRepository.save(order);
        outboxService.enqueue("ProcurementServiceOrder", saved.getId(), "procurement.service_created",
                "ProcurementServiceCreated", new Events.ProcurementServiceCreated(
                        saved.getId(), saved.getServiceType(), saved.getEquipmentId(), saved.getLocationId(), saved.getSupplierId()
                ));
        return saved;
    }

    @Transactional
    public ProcurementServiceOrder complete(Long id, BigDecimal actualCost, OffsetDateTime completedAt, java.util.UUID actDocumentId, String completedBy) {
        ProcurementServiceOrder order = serviceOrderRepository.findById(id).orElseThrow();
        String status = normalize(order.getStatus());
        if (status == null || !COMPLETABLE_STATUSES.contains(status)) {
            throw new IllegalStateException("service order not in completable status");
        }
        String serviceType = normalize(order.getServiceType());
        if (serviceType == null || !SERVICE_TYPES.contains(serviceType)) {
            throw new IllegalArgumentException("Unsupported serviceType: " + order.getServiceType());
        }
        order.setStatus("COMPLETED");
        order.setActualCost(actualCost);
        order.setActDocumentId(actDocumentId);
        order.setUpdatedAt(OffsetDateTime.now());
        ProcurementServiceOrder saved = serviceOrderRepository.save(order);
        outboxService.enqueue("ProcurementServiceOrder", saved.getId(), "procurement.service_completed",
                "ProcurementServiceCompleted", new Events.ProcurementServiceCompleted(
                        saved.getId(), saved.getServiceType(), saved.getEquipmentId(), saved.getLocationId(), saved.getSupplierId(), actualCost, actDocumentId, completedAt
                ));
        return saved;
    }

    @Transactional
    public ProcurementRfq createRfq(ProcurementRfq rfq, List<Long> supplierIds) {
        String serviceType = normalize(rfq.getServiceType());
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is required");
        }
        if (!SERVICE_TYPES.contains(serviceType)) {
            throw new IllegalArgumentException("Unsupported serviceType: " + serviceType);
        }
        rfq.setServiceType(serviceType);
        rfq.setStatus("OPEN");
        ProcurementRfq saved = rfqRepository.save(rfq);

        if (supplierIds != null) {
            for (Long supplierId : supplierIds) {
                ProcurementRfqOffer offer = new ProcurementRfqOffer();
                offer.setRfq(saved);
                offer.setSupplierId(supplierId);
                offer.setPrice(BigDecimal.ZERO);
                offer.setCurrency("RUB");
                offer.setStatus("SUBMITTED");
                rfqOfferRepository.save(offer);
            }
        }

        outboxService.enqueue("ProcurementRfq", saved.getId(), "procurement.rfq_created",
                "ProcurementRfqCreated", new Events.ProcurementRfqCreated(saved.getId(), saved.getServiceType(), saved.getEquipmentId(), saved.getLocationId()));
        return saved;
    }

    @Transactional
    public ProcurementRfqOffer addOffer(Long rfqId, ProcurementRfqOffer offer) {
        ProcurementRfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        offer.setRfq(rfq);
        ProcurementRfqOffer saved = rfqOfferRepository.save(offer);
        return saved;
    }

    @Transactional
    public ProcurementRfq award(Long rfqId, Long supplierId, String reason) {
        ProcurementRfq rfq = rfqRepository.findById(rfqId).orElseThrow();
        rfq.setStatus("AWARDED");
        rfq.setAwardedSupplierId(supplierId);
        rfq.setAwardReason(reason);
        rfq.setUpdatedAt(OffsetDateTime.now());
        ProcurementRfq saved = rfqRepository.save(rfq);

        // mark offers
        List<ProcurementRfqOffer> offers = rfqOfferRepository.findByRfqId(rfqId);
        for (ProcurementRfqOffer offer : offers) {
            if (supplierId != null && supplierId.equals(offer.getSupplierId())) {
                offer.setStatus("ACCEPTED");
            } else {
                offer.setStatus("REJECTED");
            }
            rfqOfferRepository.save(offer);
        }

        outboxService.enqueue("ProcurementRfq", saved.getId(), "procurement.rfq_awarded",
                "ProcurementRfqAwarded", new Events.ProcurementRfqAwarded(saved.getId(), supplierId, reason));
        return saved;
    }

    public List<ProcurementServiceOrder> listServiceOrders(Long equipmentId) {
        if (equipmentId == null) {
            return serviceOrderRepository.findAll();
        }
        return serviceOrderRepository.findTop50ByEquipmentIdOrderByCreatedAtDesc(equipmentId);
    }

    public Optional<ProcurementRfq> findRfq(Long id) {
        return rfqRepository.findById(id);
    }

    public List<ProcurementRfq> listRfq() {
        return rfqRepository.findAll();
    }

    public List<ProcurementRfqOffer> listOffers(Long rfqId) {
        return rfqOfferRepository.findByRfqId(rfqId);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        return v.isBlank() ? null : v;
    }
}
