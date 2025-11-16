package com.example.crp.inventory.messaging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventsTest {

    @Test
    void recordsExposeFieldsCorrectly() {
        Events.ProcurementApproved approved = new Events.ProcurementApproved(1L, 2L, 3L);
        Events.ProcurementRejected rejected = new Events.ProcurementRejected(4L, 5L, 6L);
        Events.InventoryReserved reserved = new Events.InventoryReserved(7L, 8L);
        Events.InventoryReleased released = new Events.InventoryReleased(9L, 10L);
        Events.InventoryReserveFailed reserveFailed = new Events.InventoryReserveFailed(11L, 12L, "reason");

        assertThat(approved.requestId()).isEqualTo(1L);
        assertThat(approved.equipmentId()).isEqualTo(2L);
        assertThat(approved.approverId()).isEqualTo(3L);

        assertThat(rejected.requestId()).isEqualTo(4L);
        assertThat(rejected.equipmentId()).isEqualTo(5L);
        assertThat(rejected.approverId()).isEqualTo(6L);

        assertThat(reserved.requestId()).isEqualTo(7L);
        assertThat(reserved.equipmentId()).isEqualTo(8L);

        assertThat(released.requestId()).isEqualTo(9L);
        assertThat(released.equipmentId()).isEqualTo(10L);

        assertThat(reserveFailed.requestId()).isEqualTo(11L);
        assertThat(reserveFailed.equipmentId()).isEqualTo(12L);
        assertThat(reserveFailed.reason()).isEqualTo("reason");
    }
}

