package com.example.crp.procurement;

import com.example.crp.procurement.messaging.Events;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcurementEventsContractTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void serviceCompletedEventHasStableShape() throws Exception {
        Events.ProcurementServiceCompleted event = new Events.ProcurementServiceCompleted(
                1L,
                "SERVICE_STORAGE",
                10L,
                100L,
                55L,
                new BigDecimal("950.00"),
                UUID.randomUUID(),
                OffsetDateTime.parse("2025-01-01T10:00:00Z")
        );

        String json = mapper.writeValueAsString(event);
        Map<?,?> map = mapper.readValue(json, Map.class);

        assertThat(map.containsKey("serviceOrderId")).isTrue();
        assertThat(map.containsKey("serviceType")).isTrue();
        assertThat(map.containsKey("equipmentId")).isTrue();
        assertThat(map.containsKey("locationId")).isTrue();
        assertThat(map.containsKey("supplierId")).isTrue();
        assertThat(map.containsKey("actualCost")).isTrue();
        assertThat(map.containsKey("actDocumentId")).isTrue();
        assertThat(map.containsKey("completedAt")).isTrue();
    }

    @Test
    void serviceCreatedEventHasStableShape() throws Exception {
        Events.ProcurementServiceCreated event = new Events.ProcurementServiceCreated(
                2L,
                "SERVICE_STORAGE",
                11L,
                101L,
                56L
        );

        String json = mapper.writeValueAsString(event);
        Map<?,?> map = mapper.readValue(json, Map.class);

        assertThat(map.containsKey("serviceOrderId")).isTrue();
        assertThat(map.containsKey("serviceType")).isTrue();
        assertThat(map.containsKey("equipmentId")).isTrue();
        assertThat(map.containsKey("locationId")).isTrue();
        assertThat(map.containsKey("supplierId")).isTrue();
    }
}
