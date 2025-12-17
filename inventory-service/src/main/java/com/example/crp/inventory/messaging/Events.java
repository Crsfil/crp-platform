package com.example.crp.inventory.messaging;

import java.math.BigDecimal;
import java.util.List;

public class Events {
    public record ProcurementApproved(Long requestId, Long equipmentId, Long approverId) {}
    public record ProcurementRejected(Long requestId, Long equipmentId, Long approverId) {}
    public record InventoryReserved(Long requestId, Long equipmentId) {}
    public record InventoryReleased(Long requestId, Long equipmentId) {}
    public record InventoryReserveFailed(Long requestId, Long equipmentId, String reason) {}
    public record ProcurementServiceCompleted(Long serviceOrderId,
                                              String serviceType,
                                              Long equipmentId,
                                              Long locationId,
                                              Long supplierId,
                                              java.math.BigDecimal actualCost,
                                              java.util.UUID actDocumentId,
                                              java.time.OffsetDateTime completedAt) {}
    public record ProcurementServiceCreated(Long serviceOrderId,
                                            String serviceType,
                                            Long equipmentId,
                                            Long locationId,
                                            Long supplierId) {}

    public record GoodsReceiptAccepted(Long receiptId,
                                       Long purchaseOrderId,
                                       Long requestId,
                                       Long supplierId,
                                       List<GoodsReceiptItem> items) {}

    public record GoodsReceiptItem(Long purchaseOrderLineId,
                                   Long requestLineId,
                                   String description,
                                   BigDecimal quantityReceived,
                                   String uom,
                                   BigDecimal unitPrice) {}

    public record InventoryInboundReceiptProcessed(Long receiptId,
                                                   Long purchaseOrderId,
                                                   Long requestId,
                                                   Long supplierId,
                                                   int createdEquipmentCount) {}

    public record InventoryEquipmentTransferred(Long equipmentId,
                                                Long fromLocationId,
                                                Long toLocationId,
                                                String responsibleUsername,
                                                String movedBy,
                                                String reason,
                                                String correlationId) {}

    public record InventoryEquipmentStatusChanged(Long equipmentId,
                                                  String fromStatus,
                                                  String toStatus,
                                                  String changedBy,
                                                  String reason,
                                                  String correlationId) {}

    public record InventoryEquipmentDocumentUploaded(String documentId,
                                                     Long equipmentId,
                                                     String docType,
                                                     String fileName,
                                                     String storageType,
                                                     String storageLocation,
                                                     String createdBy,
                                                     String correlationId) {}

    public record InventoryLocationCreated(Long locationId,
                                           String code,
                                           String name,
                                           String type,
                                           String region,
                                           String createdBy,
                                           String correlationId) {}

    public record InventoryEquipmentPassportUpdated(Long equipmentId,
                                                    String inventoryNumber,
                                                    String serialNumber,
                                                    String manufacturer,
                                                    String model,
                                                    String type,
                                                    String condition,
                                                    Long locationId,
                                                    String responsibleUsername) {}

    public record InventoryEquipmentLeaseStarted(Long leaseId,
                                                 Long equipmentId,
                                                 Long agreementId,
                                                 Long customerId,
                                                 Long issuedFromLocationId,
                                                 Long issuedToLocationId,
                                                 String startedBy,
                                                 String correlationId) {}

    public record InventoryEquipmentLeaseReturned(Long leaseId,
                                                  Long equipmentId,
                                                  Long returnLocationId,
                                                  String returnedBy,
                                                  String correlationId) {}

    public record InventoryEquipmentLeaseRepossessed(Long leaseId,
                                                     Long equipmentId,
                                                     Long repossessLocationId,
                                                     String repossessedBy,
                                                     String correlationId) {}

    public record InventoryRepossessionStarted(Long repossessionCaseId,
                                               Long equipmentId,
                                               Long targetLocationId,
                                               String initiatedBy,
                                               String correlationId) {}

    public record InventoryEquipmentDispositionCreated(Long dispositionId,
                                                       Long equipmentId,
                                                       String type,
                                                       String createdBy,
                                                       String correlationId) {}

    public record InventoryEquipmentDispositionApproved(Long dispositionId,
                                                        Long equipmentId,
                                                        String type,
                                                        String approvedBy,
                                                        String correlationId) {}

    public record InventoryEquipmentDispositionCompleted(Long dispositionId,
                                                         Long equipmentId,
                                                         String type,
                                                         String finalStatus,
                                                         String completedBy,
                                                         String correlationId) {}

    public record InventoryEquipmentDispositionCanceled(Long dispositionId,
                                                        Long equipmentId,
                                                        String type,
                                                        String canceledBy,
                                                        String correlationId) {}

    public record InventoryEquipmentSaleContracted(Long dispositionId,
                                                   Long equipmentId,
                                                   String saleMethod,
                                                   String lotNumber,
                                                   String contractNumber,
                                                   String contractedBy,
                                                   String correlationId) {}

    public record InventoryEquipmentSaleInvoiced(Long dispositionId,
                                                 Long equipmentId,
                                                 String invoiceNumber,
                                                 String invoicedBy,
                                                 String correlationId) {}

    public record InventoryEquipmentSalePaid(Long dispositionId,
                                             Long equipmentId,
                                             String paidBy,
                                             String correlationId) {}

    public record InventoryEquipmentDispositionDocumentLinked(Long dispositionId,
                                                              Long equipmentId,
                                                              String documentId,
                                                              String relationType,
                                                              String createdBy,
                                                              String correlationId) {}

    public record InventoryEquipmentRepairCreated(Long repairId,
                                                  Long equipmentId,
                                                  Long inspectionId,
                                                  Long repairLocationId,
                                                  String createdBy,
                                                  String correlationId) {}

    public record InventoryEquipmentRepairApproved(Long repairId,
                                                   Long equipmentId,
                                                   String approvedBy,
                                                   String correlationId) {}

    public record InventoryEquipmentRepairStarted(Long repairId,
                                                  Long equipmentId,
                                                  String startedBy,
                                                  String correlationId) {}

    public record InventoryEquipmentRepairCompleted(Long repairId,
                                                    Long equipmentId,
                                                    String completedBy,
                                                    String correlationId) {}

    public record InventoryEquipmentRepairCanceled(Long repairId,
                                                   Long equipmentId,
                                                   String canceledBy,
                                                   String correlationId) {}

    public record InventoryEquipmentRepairDocumentLinked(Long repairId,
                                                         Long equipmentId,
                                                         String documentId,
                                                         String relationType,
                                                         String createdBy,
                                                         String correlationId) {}

    public record InventoryEquipmentRepairLineAdded(Long repairId,
                                                    Long lineId,
                                                    Long equipmentId,
                                                    String kind,
                                                    java.math.BigDecimal totalCost) {}

    public record InventoryEquipmentRepairPostInspectionCreated(Long repairId,
                                                                Long inspectionId,
                                                                Long equipmentId,
                                                                String createdBy,
                                                                String correlationId) {}

    public record InventoryEquipmentInspectionCreated(Long inspectionId,
                                                      Long equipmentId,
                                                      String type,
                                                      Long locationId,
                                                      String createdBy,
                                                      String correlationId) {}

    public record InventoryEquipmentInspectionSubmitted(Long inspectionId,
                                                        Long equipmentId,
                                                        String submittedBy,
                                                        String correlationId) {}

    public record InventoryEquipmentInspectionApproved(Long inspectionId,
                                                       Long equipmentId,
                                                       String approvedBy,
                                                       String correlationId) {}

    public record InventoryEquipmentInspectionCompleted(Long inspectionId,
                                                        Long equipmentId,
                                                        String conclusion,
                                                        String recommendedAction,
                                                        String completedBy,
                                                        String correlationId) {}

    public record InventoryEquipmentInspectionFindingAdded(Long inspectionId,
                                                           Long findingId,
                                                           Long equipmentId,
                                                           String severity,
                                                           String code) {}

    public record InventoryStocktakeCreated(Long stocktakeId,
                                            Long locationId,
                                            String title,
                                            int expectedLineCount,
                                            String createdBy,
                                            String correlationId) {}

    public record InventoryStocktakeSubmitted(Long stocktakeId,
                                              Long locationId,
                                            int totalLines,
                                            int countedLines,
                                            String submittedBy,
                                            String correlationId) {}

    public record InventoryStocktakeClosed(Long stocktakeId,
                                           Long locationId,
                                           int missingCount,
                                           int movedCount,
                                           int statusMismatchCount,
                                           String closedBy,
                                           boolean applied,
                                           String correlationId) {}

    public record InventoryStorageAccepted(Long storageOrderId,
                                           Long equipmentId,
                                           Long storageLocationId,
                                           String createdBy,
                                           String correlationId) {}

    public record InventoryValuationRecorded(Long valuationId,
                                             Long equipmentId,
                                             BigDecimal valuationAmount,
                                             BigDecimal liquidationAmount,
                                             String currency,
                                             String createdBy,
                                             String correlationId) {}
}
