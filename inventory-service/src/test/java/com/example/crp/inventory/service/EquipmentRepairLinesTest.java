package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.EquipmentRepairLine;
import com.example.crp.inventory.domain.EquipmentRepairOrder;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentInspectionRepository;
import com.example.crp.inventory.repo.EquipmentRepairDocumentLinkRepository;
import com.example.crp.inventory.repo.EquipmentRepairLineRepository;
import com.example.crp.inventory.repo.EquipmentRepairOrderRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EquipmentRepairLinesTest {

    private EquipmentRepairOrderRepository repairRepository;
    private EquipmentRepairLineRepository lineRepository;
    private EquipmentRepairService service;

    @BeforeEach
    void setUp() {
        EquipmentRepository equipmentRepository = mock(EquipmentRepository.class);
        EquipmentInspectionRepository inspectionRepository = mock(EquipmentInspectionRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        repairRepository = mock(EquipmentRepairOrderRepository.class);
        EquipmentRepairDocumentLinkRepository documentLinkRepository = mock(EquipmentRepairDocumentLinkRepository.class);
        lineRepository = mock(EquipmentRepairLineRepository.class);
        EquipmentDocumentService documentService = mock(EquipmentDocumentService.class);
        EquipmentLifecycleService lifecycleService = mock(EquipmentLifecycleService.class);
        EquipmentInspectionService inspectionService = mock(EquipmentInspectionService.class);
        LocationAccessPolicy locationAccessPolicy = mock(LocationAccessPolicy.class);
        OutboxService outboxService = mock(OutboxService.class);

        service = new EquipmentRepairService(equipmentRepository, inspectionRepository, locationRepository, repairRepository,
                documentLinkRepository, lineRepository, documentService, lifecycleService, inspectionService, locationAccessPolicy, outboxService);
    }

    @Test
    void addLine_setsTotal_andUpdatesOrderActualCost() {
        EquipmentRepairOrder r = new EquipmentRepairOrder();
        r.setEquipmentId(10L);
        r.setStatus("APPROVED");
        when(repairRepository.findById(1L)).thenReturn(Optional.of(r));
        when(lineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.sumTotalCostByRepairId(1L)).thenReturn(new BigDecimal("123.45"));
        when(repairRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EquipmentRepairLine line = service.addLine(1L, "PART", "Wheel", new BigDecimal("2"), "pcs", new BigDecimal("10.00"));

        assertEquals(new BigDecimal("20.00"), line.getTotalCost());
        assertEquals(new BigDecimal("123.45"), r.getActualCost());
    }
}

