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
                row.createCell(0).setCellValue(m.get("id").toString());
                row.createCell(1).setCellValue(m.get("equipmentId").toString());
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

    private String s(Object o) { return o == null ? "" : o.toString(); }
}
