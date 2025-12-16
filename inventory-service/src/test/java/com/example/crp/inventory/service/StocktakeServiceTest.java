package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.domain.Stocktake;
import com.example.crp.inventory.domain.StocktakeLine;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.LocationRepository;
import com.example.crp.inventory.repo.StocktakeLineRepository;
import com.example.crp.inventory.repo.StocktakeRepository;
import com.example.crp.inventory.security.LocationAccessPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StocktakeServiceTest {

    private StocktakeRepository stocktakeRepository;
    private StocktakeLineRepository lineRepository;
    private EquipmentRepository equipmentRepository;
    private LocationRepository locationRepository;
    private EquipmentLifecycleService lifecycleService;
    private LocationAccessPolicy locationAccessPolicy;
    private OutboxService outboxService;
    private StocktakeService service;

    @BeforeEach
    void setUp() {
        stocktakeRepository = mock(StocktakeRepository.class);
        lineRepository = mock(StocktakeLineRepository.class);
        equipmentRepository = mock(EquipmentRepository.class);
        locationRepository = mock(LocationRepository.class);
        lifecycleService = mock(EquipmentLifecycleService.class);
        locationAccessPolicy = mock(LocationAccessPolicy.class);
        outboxService = mock(OutboxService.class);
        service = new StocktakeService(stocktakeRepository, lineRepository, equipmentRepository, locationRepository, lifecycleService, locationAccessPolicy, outboxService);
    }

    @Test
    void create_snapshotsEquipmentInLocation_andEnqueuesEvent() {
        Location loc = new Location();
        loc.setRegion("MSK");
        when(locationRepository.findById(10L)).thenReturn(Optional.of(loc));

        when(stocktakeRepository.save(any(Stocktake.class))).thenAnswer(inv -> {
            Stocktake st = inv.getArgument(0);
            st.setStatus("OPEN");
            // emulate DB id
            try {
                java.lang.reflect.Field f = Stocktake.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(st, 1L);
            } catch (Exception ignored) {}
            return st;
        });

        Equipment e1 = new Equipment();
        e1.setId(100L);
        e1.setInventoryNumber("INV-100");
        e1.setLocationId(10L);
        e1.setStatus("AVAILABLE");
        Equipment e2 = new Equipment();
        e2.setId(200L);
        e2.setInventoryNumber("INV-200");
        e2.setLocationId(10L);
        e2.setStatus("IN_STORAGE");
        when(equipmentRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Equipment>>any()))
                .thenReturn(List.of(e1, e2));

        Authentication auth = mock(Authentication.class);
        Stocktake st = service.create(10L, "Count", "u1", "corr", auth);

        assertNotNull(st);
        assertEquals(10L, st.getLocationId());
        verify(lineRepository).saveAll(argThat(lines -> ((List<?>) lines).size() == 2));
        verify(outboxService).enqueue(eq("Stocktake"), anyLong(), eq("inventory.stocktake.created"), eq("InventoryStocktakeCreated"), any());
    }

    @Test
    void submit_requiresOpen_andEnqueuesEvent() {
        Stocktake st = new Stocktake();
        st.setLocationId(10L);
        st.setStatus("OPEN");
        when(stocktakeRepository.findById(1L)).thenReturn(Optional.of(st));
        when(stocktakeRepository.save(any())).thenAnswer(inv -> {
            Stocktake saved = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = Stocktake.class.getDeclaredField("id");
                f.setAccessible(true);
                if (f.get(saved) == null) {
                    f.set(saved, 1L);
                }
            } catch (Exception ignored) {}
            return saved;
        });
        StocktakeLine l1 = new StocktakeLine();
        l1.setCountedAt(java.time.OffsetDateTime.now());
        StocktakeLine l2 = new StocktakeLine();
        when(lineRepository.findByStocktakeIdOrderByIdAsc(1L)).thenReturn(List.of(l1, l2));

        Stocktake saved = service.submit(1L, "u1", "corr");

        assertEquals("SUBMITTED", saved.getStatus());
        verify(outboxService).enqueue(eq("Stocktake"), anyLong(), eq("inventory.stocktake.submitted"), eq("InventoryStocktakeSubmitted"), any());
    }

    @Test
    void close_applyCallsLifecycleService() {
        Stocktake st = new Stocktake();
        st.setLocationId(10L);
        st.setStatus("SUBMITTED");
        when(stocktakeRepository.findById(1L)).thenReturn(Optional.of(st));
        when(stocktakeRepository.save(any())).thenAnswer(inv -> {
            Stocktake saved = inv.getArgument(0);
            try {
                java.lang.reflect.Field f = Stocktake.class.getDeclaredField("id");
                f.setAccessible(true);
                if (f.get(saved) == null) {
                    f.set(saved, 1L);
                }
            } catch (Exception ignored) {}
            return saved;
        });
        Location loc = new Location();
        loc.setRegion("MSK");
        when(locationRepository.findById(10L)).thenReturn(Optional.of(loc));

        StocktakeLine line = new StocktakeLine();
        line.setEquipmentId(100L);
        line.setExpectedLocationId(10L);
        line.setCountedAt(java.time.OffsetDateTime.now());
        line.setCountedPresent(true);
        line.setCountedLocationId(20L);
        line.setExpectedStatus("AVAILABLE");
        line.setCountedStatus("IN_STORAGE");
        when(lineRepository.findByStocktakeIdOrderByIdAsc(1L)).thenReturn(List.of(line));

        Authentication auth = mock(Authentication.class);
        StocktakeService.CloseResult res = service.close(1L, true, "u1", "corr", auth);

        assertTrue(res.applied());
        verify(lifecycleService).transfer(eq(100L), eq(20L), any(), eq("stocktake"), eq("u1"), eq("corr"), eq(auth));
        verify(lifecycleService).changeStatus(eq(100L), eq("IN_STORAGE"), eq("stocktake"), eq("u1"), eq("corr"), eq(auth));
        verify(outboxService).enqueue(eq("Stocktake"), anyLong(), eq("inventory.stocktake.closed"), eq("InventoryStocktakeClosed"), any());
    }
}
