package com.example.crp.procurement;

import com.example.crp.procurement.config.TestSecurityConfig;
import com.example.crp.procurement.config.TestcontainersDisabler;
import com.example.crp.procurement.domain.ProcurementServiceOrder;
import com.example.crp.procurement.outbox.OutboxEventRepository;
import com.example.crp.procurement.service.ProcurementServiceOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,com.example.crp.security.JwtAudienceAutoConfiguration",
        "spring.task.scheduling.enabled=false",
        "spring.main.allow-bean-definition-overriding=true",
        "procurement.security.abac.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(TestcontainersDisabler.class)
@Import(TestSecurityConfig.class)
class ProcurementServiceOrderPostgresTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("procdb")
                    .withUsername("proc")
                    .withPassword("proc");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("DB_URL", postgres::getJdbcUrl);
        registry.add("DB_USERNAME", postgres::getUsername);
        registry.add("DB_PASSWORD", postgres::getPassword);
    }

    @Autowired
    private ProcurementServiceOrderService service;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void completingServiceOrderEnqueuesOutboxEvent() {
        ProcurementServiceOrder order = new ProcurementServiceOrder();
        order.setServiceType("SERVICE_STORAGE");
        order.setEquipmentId(10L);
        order.setLocationId(100L);
        order.setPlannedCost(new BigDecimal("1000.00"));
        order.setCurrency("RUB");

        ProcurementServiceOrder created = service.create(order, null);
        service.complete(created.getId(), new BigDecimal("950.00"), OffsetDateTime.now(), null, "tester", null);

        var events = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        assertThat(events.stream().anyMatch(e -> "procurement.service_completed".equals(e.getTopic()))).isTrue();
    }
}
