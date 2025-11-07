package com.example.crp.procurement.messaging;

public class Events {
    public record ProcurementRequested(Long requestId, Long equipmentId, Long requesterId) {}
    public record ProcurementApproved(Long requestId, Long equipmentId, Long approverId) {}
    public record ProcurementRejected(Long requestId, Long equipmentId, Long approverId) {}
    public record InventoryReserved(Long requestId, Long equipmentId) {}
    public record InventoryReserveFailed(Long requestId, Long equipmentId, String reason) {}
    public record InventoryReleased(Long requestId, Long equipmentId) {}
}

