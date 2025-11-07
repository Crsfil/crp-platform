package com.example.crp.reports.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReportGenerator {
    private final RestClient inventoryClient;
    private final RestClient procurementClient;
    private final String internalApiKey;

    public ReportGenerator(@Value("${inventory.base-url:http://inventory-service:8082}") String inventoryBase,
                           @Value("${procurement.base-url:http://procurement-service:8083}") String procurementBase,
                           @Value("${security.internal-api-key:}") String internalApiKey) {
        this.inventoryClient = RestClient.builder().baseUrl(inventoryBase).build();
        this.procurementClient = RestClient.builder().baseUrl(procurementBase).build();
        this.internalApiKey = internalApiKey;
    }

    public byte[] equipmentByStatusXlsx() {
        List<Map<String, Object>> equipment = inventoryClient.get().uri("/equipment")
                .header("X-Internal-API-Key", internalApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(List.class);
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
                .header("X-Internal-API-Key", internalApiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().body(List.class);
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

    private String s(Object o) { return o == null ? "" : o.toString(); }
}

