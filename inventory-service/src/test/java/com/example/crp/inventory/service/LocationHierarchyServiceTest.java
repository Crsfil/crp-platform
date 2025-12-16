package com.example.crp.inventory.service;

import com.example.crp.inventory.domain.Location;
import com.example.crp.inventory.outbox.OutboxService;
import com.example.crp.inventory.repo.LocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocationHierarchyServiceTest {

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
    void reparent_rewritesSubtreePaths_andPreventsCycles() {
        Location root = new Location();
        root.setCode("ROOT");
        root.setPath("ROOT");
        root.setLevel(0);

        Location child = new Location();
        child.setCode("CHILD");
        child.setPath("ROOT/CHILD");
        child.setLevel(1);

        Location leaf = new Location();
        leaf.setCode("LEAF");
        leaf.setPath("ROOT/CHILD/LEAF");
        leaf.setLevel(2);

        Location newParent = new Location();
        newParent.setCode("NEW");
        newParent.setPath("NEW");
        newParent.setLevel(0);

        when(repository.findById(1L)).thenReturn(Optional.of(child));
        when(repository.findById(2L)).thenReturn(Optional.of(newParent));
        when(repository.findByPathStartingWithOrderByPathAsc("ROOT/CHILD/")).thenReturn(List.of(leaf));
        when(repository.save(any(Location.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        Location updated = service.reparent(1L, 2L);
        assertEquals("NEW/CHILD", updated.getPath());
        assertEquals(1, updated.getLevel());
        assertEquals(2L, updated.getParentId());
        assertEquals("NEW/CHILD/LEAF", leaf.getPath());
        assertEquals(2, leaf.getLevel());

        // cycle: try to parent CHILD under its own descendant (LEAF)
        when(repository.findById(1L)).thenReturn(Optional.of(child));
        when(repository.findById(4L)).thenReturn(Optional.of(leaf));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.reparent(1L, 4L));
        assertTrue(ex.getMessage().toLowerCase().contains("descendant"));
    }
}
