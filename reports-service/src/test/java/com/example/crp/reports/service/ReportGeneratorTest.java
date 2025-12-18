package com.example.crp.reports.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorTest {

    @Test
    void repossessedPortfolioXlsx_containsOnlyMatchingStatuses() throws Exception {
        Map<String, String> inventoryResponses = Map.of(
                "/equipment",
                "[" +
                        "{\"id\":1,\"status\":\"REPOSSESSION_PENDING\",\"type\":\"TRUCK\",\"model\":\"X\",\"branchCode\":\"MSK\",\"currentCustodian\":\"MSK\",\"valuationAmount\":1000,\"valuationAt\":\"2025-01-01\"}," +
                        "{\"id\":2,\"status\":\"AVAILABLE\",\"type\":\"CAR\",\"model\":\"Y\"}" +
                        "]"
        );
        ReportGenerator generator = new ReportGenerator(
                stubClient(inventoryResponses),
                stubClient(inventoryResponses),
                stubClient(Map.of("/service/orders", "[]")),
                stubClient(Map.of("/service/orders", "[]")),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of())
        );

        byte[] bytes = generator.repossessedPortfolioXlsx();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Repossessed");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("1");
        }
    }

    @Test
    void storageCostsXlsx_containsServiceOrders() throws Exception {
        Map<String, String> procurementResponses = Map.of(
                "/service/orders",
                "[" +
                        "{\"id\":10,\"equipmentId\":4,\"serviceType\":\"SERVICE_STORAGE\",\"locationId\":2,\"status\":\"COMPLETED\",\"plannedCost\":500,\"actualCost\":450,\"currency\":\"RUB\",\"slaUntil\":\"2025-01-02\"}" +
                        "]"
        );
        ReportGenerator generator = new ReportGenerator(
                stubClient(Map.of("/equipment", "[]")),
                stubClient(Map.of("/equipment", "[]")),
                stubClient(procurementResponses),
                stubClient(procurementResponses),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of())
        );

        byte[] bytes = generator.storageCostsXlsx();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("StorageCosts");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("10");
        }
    }

    @Test
    void dispositionResultsXlsx_flattensDispositions() throws Exception {
        Map<String, String> inventoryResponses = new HashMap<>();
        inventoryResponses.put("/equipment", "[{\"id\":7,\"status\":\"SALE_LISTED\"}]");
        inventoryResponses.put("/equipment/7/dispositions",
                "[" +
                        "{\"id\":33,\"equipmentId\":7,\"type\":\"SALE\",\"status\":\"PAID\",\"plannedPrice\":1000,\"actualPrice\":900," +
                        "\"currency\":\"RUB\",\"counterpartyName\":\"Buyer\",\"saleMethod\":\"auction\",\"lotNumber\":\"L1\"," +
                        "\"contractNumber\":\"C1\",\"invoiceNumber\":\"I1\",\"paidAt\":\"2025-01-05\",\"performedAt\":\"2025-01-06\",\"locationId\":2}" +
                        "]"
        );
        ReportGenerator generator = new ReportGenerator(
                stubClient(inventoryResponses),
                stubClient(inventoryResponses),
                stubClient(Map.of("/service/orders", "[]")),
                stubClient(Map.of("/service/orders", "[]")),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of()),
                stubClient(Map.of())
        );

        byte[] bytes = generator.dispositionResultsXlsx();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("DispositionResults");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("33");
        }
    }

    private WebClient stubClient(Map<String, String> responses) {
        ExchangeFunction exchange = request -> Mono.just(buildResponse(request, responses));
        return WebClient.builder().exchangeFunction(exchange).build();
    }

    private ClientResponse buildResponse(ClientRequest request, Map<String, String> responses) {
        String path = request.url().getPath();
        String json = responses.getOrDefault(path, "[]");
        return ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(json)
                .build();
    }
}
