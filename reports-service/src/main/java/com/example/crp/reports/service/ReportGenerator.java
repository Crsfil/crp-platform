package com.example.crp.reports.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerator {
    private final WebClient inventoryClient;
    private final WebClient procurementClient;
    private final WebClient agreementClient;
    private final WebClient billingClient;
    private final WebClient customerClient;
    private final WebClient applicationClient;

    public ReportGenerator(WebClient inventoryClient,
                           WebClient procurementClient,
                           WebClient agreementClient,
                           WebClient billingClient,
                           WebClient customerClient,
                           WebClient applicationClient) {
        this.inventoryClient = inventoryClient;
        this.procurementClient = procurementClient;
        this.agreementClient = agreementClient;
        this.billingClient = billingClient;
        this.customerClient = customerClient;
        this.applicationClient = applicationClient;
    }

    public byte[] equipmentByStatusXlsx() {
        List<Map<String, Object>> equipment = inventoryClient.get().uri("/equipment")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Equipment");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("TYPE");
            header.createCell(2).setCellValue("MODEL");
            header.createCell(3).setCellValue("STATUS");
            header.createCell(4).setCellValue("PRICE");
            for (Object o : equipment) {
                Map<?,?> m = (Map<?,?>) o;
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(m.get("id").toString());
                row.createCell(1).setCellValue(s(m.get("type")));
                row.createCell(2).setCellValue(s(m.get("model")));
                row.createCell(3).setCellValue(s(m.get("status")));
                row.createCell(4).setCellValue(m.get("price") == null ? "" : m.get("price").toString());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] requestsByStatusXlsx() {
        List<Map<String, Object>> reqs = procurementClient.get().uri("/requests")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Requests");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("EQUIPMENT_ID");
            header.createCell(2).setCellValue("REQUESTER_ID");
            header.createCell(3).setCellValue("STATUS");
            for (Object o : reqs) {
                Map<?,?> m = (Map<?,?>) o;
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s(m.get("id")));
                row.createCell(1).setCellValue(s(m.get("equipmentId")));
                row.createCell(2).setCellValue(m.get("requesterId") == null ? "" : m.get("requesterId").toString());
                row.createCell(3).setCellValue(s(m.get("status")));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public byte[] agreementsPortfolioXlsx() {
        List<Map<String, Object>> agreements = agreementClient.get().uri("/agreements")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        List<Map<String, Object>> customers = customerClient.get().uri("/customers")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        Map<Object, Map<String, Object>> customersById = new HashMap<>();
        if (customers != null) {
            for (Map<String, Object> c : customers) {
                customersById.put(c.get("id"), c);
            }
        }
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Agreements");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("AGREEMENT_ID");
            header.createCell(1).setCellValue("NUMBER");
            header.createCell(2).setCellValue("STATUS");
            header.createCell(3).setCellValue("CUSTOMER_ID");
            header.createCell(4).setCellValue("CUSTOMER_NAME");
            header.createCell(5).setCellValue("AMOUNT");
            header.createCell(6).setCellValue("TERM_MONTHS");
            for (Object o : agreements) {
                Map<?, ?> a = (Map<?, ?>) o;
                Row row = sheet.createRow(r++);
                Object customerId = a.get("customerId");
                Map<String, Object> customer = customersById.get(customerId);
                row.createCell(0).setCellValue(s(a.get("id")));
                row.createCell(1).setCellValue(s(a.get("number")));
                row.createCell(2).setCellValue(s(a.get("status")));
                row.createCell(3).setCellValue(customerId == null ? "" : customerId.toString());
                row.createCell(4).setCellValue(customer == null ? "" : s(customer.get("name")));
                row.createCell(5).setCellValue(s(a.get("amount")));
                row.createCell(6).setCellValue(s(a.get("termMonths")));
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] invoicesAgingXlsx() {
        List<Map<String, Object>> invoices = billingClient.get().uri("/invoices")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        LocalDate today = LocalDate.now();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Aging");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("INVOICE_ID");
            header.createCell(1).setCellValue("AGREEMENT_ID");
            header.createCell(2).setCellValue("DUE_DATE");
            header.createCell(3).setCellValue("AMOUNT");
            header.createCell(4).setCellValue("STATUS");
            header.createCell(5).setCellValue("DAYS_OVERDUE");
            header.createCell(6).setCellValue("BUCKET");
            DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE;
            for (Object o : invoices) {
                Map<?, ?> inv = (Map<?, ?>) o;
                String status = s(inv.get("status"));
                String dueDateStr = s(inv.get("dueDate"));
                LocalDate dueDate = dueDateStr.isEmpty() ? null : LocalDate.parse(dueDateStr);
                long daysOverdue = 0;
                String bucket = "";
                if (dueDate != null && !"PAID".equalsIgnoreCase(status)) {
                    daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
                    if (daysOverdue <= 0) {
                        bucket = "CURRENT";
                    } else if (daysOverdue <= 30) {
                        bucket = "1-30";
                    } else if (daysOverdue <= 60) {
                        bucket = "31-60";
                    } else if (daysOverdue <= 90) {
                        bucket = "61-90";
                    } else {
                        bucket = "90+";
                    }
                }
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s(inv.get("id")));
                row.createCell(1).setCellValue(s(inv.get("agreementId")));
                row.createCell(2).setCellValue(dueDate == null ? "" : fmt.format(dueDate));
                row.createCell(3).setCellValue(s(inv.get("amount")));
                row.createCell(4).setCellValue(status);
                row.createCell(5).setCellValue(daysOverdue);
                row.createCell(6).setCellValue(bucket);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] invoicesCashflowXlsx() {
        List<Map<String, Object>> invoices = billingClient.get().uri("/invoices")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        Map<String, Double> byMonth = new HashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        if (invoices != null) {
            for (Object o : invoices) {
                Map<?, ?> inv = (Map<?, ?>) o;
                String status = s(inv.get("status"));
                String dueDateStr = s(inv.get("dueDate"));
                if (!"PAID".equalsIgnoreCase(status) && !dueDateStr.isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    String key = fmt.format(dueDate);
                    Double amount = inv.get("amount") == null ? 0.0 : ((Number) inv.get("amount")).doubleValue();
                    byMonth.merge(key, amount, Double::sum);
                }
            }
        }
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Cashflow");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("MONTH");
            header.createCell(1).setCellValue("PLANNED_AMOUNT");
            for (var entry : byMonth.entrySet()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] applicationsKpiXlsx() {
        List<Map<String, Object>> apps = applicationClient.get().uri("/applications")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        Map<String, Long> byStatus = new HashMap<>();
        if (apps != null) {
            for (Object o : apps) {
                Map<?, ?> a = (Map<?, ?>) o;
                String status = s(a.get("status"));
                String key = status.isEmpty() ? "UNKNOWN" : status;
                Long current = byStatus.getOrDefault(key, 0L);
                byStatus.put(key, current + 1L);
            }
        }
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("ApplicationsKPI");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("STATUS");
            header.createCell(1).setCellValue("COUNT");
            for (var entry : byStatus.entrySet()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] procurementPipelineXlsx() {
        List<Map<String, Object>> requests = procurementClient.get().uri("/requests")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        List<Map<String, Object>> purchaseOrders = procurementClient.get().uri("/purchase-orders")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        List<Map<String, Object>> receipts = procurementClient.get().uri("/receipts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        List<Map<String, Object>> suppliers = procurementClient.get().uri("/suppliers")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();

        Map<Long, String> supplierNameById = new HashMap<>();
        if (suppliers != null) {
            for (Map<String, Object> s : suppliers) {
                Long id = nLong(s.get("id"));
                if (id != null) {
                    supplierNameById.put(id, this.s(s.get("name")));
                }
            }
        }

        Map<Long, List<Map<String, Object>>> poByRequestId = new HashMap<>();
        if (purchaseOrders != null) {
            for (Map<String, Object> po : purchaseOrders) {
                Long requestId = nLong(po.get("requestId"));
                if (requestId == null) continue;
                poByRequestId.computeIfAbsent(requestId, k -> new java.util.ArrayList<>()).add(po);
            }
        }

        Map<Long, List<Map<String, Object>>> receiptsByPoId = new HashMap<>();
        if (receipts != null) {
            for (Map<String, Object> gr : receipts) {
                Long poId = nLong(gr.get("purchaseOrderId"));
                if (poId == null) continue;
                receiptsByPoId.computeIfAbsent(poId, k -> new java.util.ArrayList<>()).add(gr);
            }
        }

        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("ProcurementPipeline");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("REQUEST_ID");
            header.createCell(1).setCellValue("KIND");
            header.createCell(2).setCellValue("REQUEST_STATUS");
            header.createCell(3).setCellValue("REQUEST_AMOUNT");
            header.createCell(4).setCellValue("REQUEST_CREATED_AT");
            header.createCell(5).setCellValue("PO_ID");
            header.createCell(6).setCellValue("PO_STATUS");
            header.createCell(7).setCellValue("SUPPLIER_ID");
            header.createCell(8).setCellValue("SUPPLIER_NAME");
            header.createCell(9).setCellValue("PO_TOTAL");
            header.createCell(10).setCellValue("PO_CREATED_AT");
            header.createCell(11).setCellValue("RECEIPT_ID");
            header.createCell(12).setCellValue("RECEIPT_STATUS");
            header.createCell(13).setCellValue("RECEIPT_CREATED_AT");

            if (requests != null) {
                for (Object o : requests) {
                    Map<?, ?> req = (Map<?, ?>) o;
                    Long requestId = nLong(req.get("id"));
                    List<Map<String, Object>> pos = requestId == null ? List.of() : poByRequestId.getOrDefault(requestId, List.of());
                    if (pos.isEmpty()) {
                        Row row = sheet.createRow(r++);
                        row.createCell(0).setCellValue(s(requestId));
                        row.createCell(1).setCellValue(s(req.get("kind")));
                        row.createCell(2).setCellValue(s(req.get("status")));
                        row.createCell(3).setCellValue(s(req.get("amount")));
                        row.createCell(4).setCellValue(s(req.get("createdAt")));
                        continue;
                    }
                    for (Map<String, Object> po : pos) {
                        Long poId = nLong(po.get("id"));
                        Long supplierId = nLong(po.get("supplierId"));
                        String supplierName = supplierId == null ? "" : supplierNameById.getOrDefault(supplierId, "");
                        List<Map<String, Object>> grs = poId == null ? List.of() : receiptsByPoId.getOrDefault(poId, List.of());
                        if (grs.isEmpty()) {
                            Row row = sheet.createRow(r++);
                            row.createCell(0).setCellValue(s(requestId));
                            row.createCell(1).setCellValue(s(req.get("kind")));
                            row.createCell(2).setCellValue(s(req.get("status")));
                            row.createCell(3).setCellValue(s(req.get("amount")));
                            row.createCell(4).setCellValue(s(req.get("createdAt")));
                            row.createCell(5).setCellValue(s(poId));
                            row.createCell(6).setCellValue(s(po.get("status")));
                            row.createCell(7).setCellValue(s(supplierId));
                            row.createCell(8).setCellValue(supplierName);
                            row.createCell(9).setCellValue(s(po.get("totalAmount")));
                            row.createCell(10).setCellValue(s(po.get("createdAt")));
                            continue;
                        }
                        for (Map<String, Object> gr : grs) {
                            Row row = sheet.createRow(r++);
                            row.createCell(0).setCellValue(s(requestId));
                            row.createCell(1).setCellValue(s(req.get("kind")));
                            row.createCell(2).setCellValue(s(req.get("status")));
                            row.createCell(3).setCellValue(s(req.get("amount")));
                            row.createCell(4).setCellValue(s(req.get("createdAt")));
                            row.createCell(5).setCellValue(s(poId));
                            row.createCell(6).setCellValue(s(po.get("status")));
                            row.createCell(7).setCellValue(s(supplierId));
                            row.createCell(8).setCellValue(supplierName);
                            row.createCell(9).setCellValue(s(po.get("totalAmount")));
                            row.createCell(10).setCellValue(s(po.get("createdAt")));
                            row.createCell(11).setCellValue(s(gr.get("id")));
                            row.createCell(12).setCellValue(s(gr.get("status")));
                            row.createCell(13).setCellValue(s(gr.get("createdAt")));
                        }
                    }
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] supplierSpendXlsx() {
        List<Map<String, Object>> purchaseOrders = procurementClient.get().uri("/purchase-orders")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        List<Map<String, Object>> suppliers = procurementClient.get().uri("/suppliers")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();

        Map<Long, String> supplierNameById = new HashMap<>();
        if (suppliers != null) {
            for (Map<String, Object> s : suppliers) {
                Long id = nLong(s.get("id"));
                if (id != null) {
                    supplierNameById.put(id, this.s(s.get("name")));
                }
            }
        }

        Map<Long, java.math.BigDecimal> totalBySupplier = new HashMap<>();
        Map<Long, Long> countBySupplier = new HashMap<>();
        if (purchaseOrders != null) {
            for (Map<String, Object> po : purchaseOrders) {
                Long supplierId = nLong(po.get("supplierId"));
                if (supplierId == null) continue;
                java.math.BigDecimal total = nBigDecimal(po.get("totalAmount"));
                totalBySupplier.merge(supplierId, total, java.math.BigDecimal::add);
                countBySupplier.merge(supplierId, 1L, Long::sum);
            }
        }

        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("SupplierSpend");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("SUPPLIER_ID");
            header.createCell(1).setCellValue("SUPPLIER_NAME");
            header.createCell(2).setCellValue("PO_COUNT");
            header.createCell(3).setCellValue("TOTAL_AMOUNT");

            for (var entry : totalBySupplier.entrySet()) {
                Long supplierId = entry.getKey();
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(s(supplierId));
                row.createCell(1).setCellValue(supplierNameById.getOrDefault(supplierId, ""));
                row.createCell(2).setCellValue(countBySupplier.getOrDefault(supplierId, 0L));
                row.createCell(3).setCellValue(entry.getValue().toPlainString());
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] repossessedPortfolioXlsx() {
        List<Map<String, Object>> equipment = inventoryClient.get().uri("/equipment")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        var statuses = List.of("REPOSSESSION_PENDING", "IN_TRANSIT", "IN_STORAGE", "UNDER_EVALUATION", "UNDER_REPAIR", "SALE_LISTED", "REPOSSESSED");
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Repossessed");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("STATUS");
            header.createCell(2).setCellValue("TYPE");
            header.createCell(3).setCellValue("MODEL");
            header.createCell(4).setCellValue("BRANCH");
            header.createCell(5).setCellValue("CUSTODIAN");
            header.createCell(6).setCellValue("VALUATION");
            header.createCell(7).setCellValue("VALUATION_AT");
            if (equipment != null) {
                for (Object o : equipment) {
                    Map<?,?> m = (Map<?,?>) o;
                    String st = s(m.get("status")).toUpperCase();
                    if (!statuses.contains(st)) continue;
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(s(m.get("id")));
                    row.createCell(1).setCellValue(st);
                    row.createCell(2).setCellValue(s(m.get("type")));
                    row.createCell(3).setCellValue(s(m.get("model")));
                    row.createCell(4).setCellValue(s(m.get("branchCode")));
                    row.createCell(5).setCellValue(s(m.get("currentCustodian")));
                    row.createCell(6).setCellValue(s(m.get("valuationAmount")));
                    row.createCell(7).setCellValue(s(m.get("valuationAt")));
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] storageCostsXlsx() {
        List<Map<String, Object>> orders = procurementClient.get().uri("/service/orders")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("StorageCosts");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("ORDER_ID");
            header.createCell(1).setCellValue("EQUIPMENT_ID");
            header.createCell(2).setCellValue("SERVICE_TYPE");
            header.createCell(3).setCellValue("LOCATION_ID");
            header.createCell(4).setCellValue("STATUS");
            header.createCell(5).setCellValue("PLANNED_COST");
            header.createCell(6).setCellValue("ACTUAL_COST");
            header.createCell(7).setCellValue("CURRENCY");
            header.createCell(8).setCellValue("SLA_UNTIL");
            if (orders != null) {
                for (Object o : orders) {
                    Map<?,?> m = (Map<?,?>) o;
                    Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(s(m.get("id")));
                    row.createCell(1).setCellValue(s(m.get("equipmentId")));
                    row.createCell(2).setCellValue(s(m.get("serviceType")));
                    row.createCell(3).setCellValue(s(m.get("locationId")));
                    row.createCell(4).setCellValue(s(m.get("status")));
                    row.createCell(5).setCellValue(s(m.get("plannedCost")));
                    row.createCell(6).setCellValue(s(m.get("actualCost")));
                    row.createCell(7).setCellValue(s(m.get("currency")));
                    row.createCell(8).setCellValue(s(m.get("slaUntil")));
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] dispositionResultsXlsx() {
        List<Map<String, Object>> equipment = inventoryClient.get().uri("/equipment")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(List.class)
                .block();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("DispositionResults");
            int r = 0;
            Row header = sheet.createRow(r++);
            header.createCell(0).setCellValue("DISPOSITION_ID");
            header.createCell(1).setCellValue("EQUIPMENT_ID");
            header.createCell(2).setCellValue("TYPE");
            header.createCell(3).setCellValue("STATUS");
            header.createCell(4).setCellValue("PLANNED_PRICE");
            header.createCell(5).setCellValue("ACTUAL_PRICE");
            header.createCell(6).setCellValue("CURRENCY");
            header.createCell(7).setCellValue("COUNTERPARTY");
            header.createCell(8).setCellValue("SALE_METHOD");
            header.createCell(9).setCellValue("LOT_NUMBER");
            header.createCell(10).setCellValue("CONTRACT_NUMBER");
            header.createCell(11).setCellValue("INVOICE_NUMBER");
            header.createCell(12).setCellValue("PAID_AT");
            header.createCell(13).setCellValue("PERFORMED_AT");
            header.createCell(14).setCellValue("LOCATION_ID");

            if (equipment != null) {
                for (Map<String, Object> e : equipment) {
                    Long equipmentId = nLong(e.get("id"));
                    if (equipmentId == null) {
                        continue;
                    }
                    List<Map<String, Object>> dispositions;
                    try {
                        dispositions = inventoryClient.get()
                                .uri("/equipment/{id}/dispositions", equipmentId)
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(List.class)
                                .block();
                    } catch (Exception ex) {
                        continue;
                    }
                    if (dispositions == null || dispositions.isEmpty()) {
                        continue;
                    }
                    for (Map<String, Object> d : dispositions) {
                        Row row = sheet.createRow(r++);
                        row.createCell(0).setCellValue(s(d.get("id")));
                        row.createCell(1).setCellValue(s(d.get("equipmentId")));
                        row.createCell(2).setCellValue(s(d.get("type")));
                        row.createCell(3).setCellValue(s(d.get("status")));
                        row.createCell(4).setCellValue(s(d.get("plannedPrice")));
                        row.createCell(5).setCellValue(s(d.get("actualPrice")));
                        row.createCell(6).setCellValue(s(d.get("currency")));
                        row.createCell(7).setCellValue(s(d.get("counterpartyName")));
                        row.createCell(8).setCellValue(s(d.get("saleMethod")));
                        row.createCell(9).setCellValue(s(d.get("lotNumber")));
                        row.createCell(10).setCellValue(s(d.get("contractNumber")));
                        row.createCell(11).setCellValue(s(d.get("invoiceNumber")));
                        row.createCell(12).setCellValue(s(d.get("paidAt")));
                        row.createCell(13).setCellValue(s(d.get("performedAt")));
                        row.createCell(14).setCellValue(s(d.get("locationId")));
                    }
                }
            }
            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Long nLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static java.math.BigDecimal nBigDecimal(Object o) {
        if (o == null) return java.math.BigDecimal.ZERO;
        if (o instanceof java.math.BigDecimal bd) return bd;
        if (o instanceof Number n) return java.math.BigDecimal.valueOf(n.doubleValue());
        try {
            return new java.math.BigDecimal(o.toString());
        } catch (Exception ignored) {
            return java.math.BigDecimal.ZERO;
        }
    }

    private String s(Object o) { return o == null ? "" : o.toString(); }
}
