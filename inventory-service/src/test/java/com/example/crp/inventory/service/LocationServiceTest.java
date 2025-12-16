package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LocationServiceTest {

    private LocationRepository repository;
    private OutboxService outboxService;
    private LocationService service;

    @BeforeEach
    void setUp() {
        repository = mock(LocationRepository.class);
        outboxService = mock(OutboxService.class);
        service = new LocationService(repository, outboxService);
    }

    @Test
    void create_trimsCode_defaultsType_andEnqueuesOutbox() {
        when(repository.findByCode("MSK-WH-1")).thenReturn(Optional.empty());
        when(repository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

        Location loc = new Location();
        loc.setCode(" MSK-WH-1 ");
        loc.setName("Main");
        Location saved = service.create(loc, "u1", "corr-1");

        assertEquals("MSK-WH-1", saved.getCode());
        assertEquals("OTHER", saved.getType());
        assertEquals("ACTIVE", saved.getStatus());
        verify(outboxService).enqueue(eq("Location"), any(), eq("inventory.location.created"), eq("InventoryLocationCreated"), any());
    }

    @Test
    void create_rejectsDuplicateCode() {
        Location existing = new Location();
        existing.setCode("MSK-WH-1");
        when(repository.findByCode("MSK-WH-1")).thenReturn(Optional.of(existing));

        Location loc = new Location();
        loc.setCode("MSK-WH-1");
        loc.setName("Main");

        assertThrows(IllegalStateException.class, () -> service.create(loc, "u1", "corr-1"));
        verify(repository, never()).save(any());
        verify(outboxService, never()).enqueue(anyString(), anyLong(), anyString(), anyString(), any());
    }

    @Test
    void setStatus_uppercases() {
        Location loc = new Location();
        loc.setCode("MSK");
        loc.setName("Main");
        loc.setStatus("ACTIVE");
        when(repository.findById(10L)).thenReturn(Optional.of(loc));
        when(repository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));

        Location saved = service.setStatus(10L, "inactive");

        assertEquals("INACTIVE", saved.getStatus());
    }
}
