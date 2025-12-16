package com.example.crp.inventory.service;

import com.example.crp.inventory.config.TestcontainersDisabler;
import com.example.crp.inventory.config.TestSecurityConfig;
import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.messaging.Events;
import com.example.crp.inventory.outbox.OutboxEventRepository;
import com.example.crp.inventory.repo.EquipmentRepository;
import com.example.crp.inventory.repo.EquipmentReservationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,com.example.crp.security.JwtAudienceAutoConfiguration",
        "outbox.publisher.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(TestcontainersDisabler.class)
@Import(TestSecurityConfig.class)
class InventoryReservationServicePostgresTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("inventorydb")
                    .withUsername("inventory")
                    .withPassword("inventory");

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
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private InventoryReservationService reservationService;

    @Test
    void reserve_isIdempotent_andWritesOutbox() {
        outboxRepository.deleteAll();
        reservationRepository.deleteAll();
        equipmentRepository.deleteAll();

        Equipment equipment = new Equipment();
        equipment.setModel("KamAZ");
        equipment.setStatus("AVAILABLE");
        equipment = equipmentRepository.save(equipment);

        Events.ProcurementApproved approved = new Events.ProcurementApproved(100L, equipment.getId(), 1L);
        reservationService.reserveFromProcurementApproval(approved);
        reservationService.reserveFromProcurementApproval(approved);

        Equipment updated = equipmentRepository.findById(equipment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualToIgnoringCase("RESERVED");

        assertThat(reservationRepository.findByRequestIdAndStatus(100L, "ACTIVE")).isPresent();

        assertThat(outboxRepository.findAll())
                .filteredOn(e -> e.getAggregateId().equals(100L))
                .filteredOn(e -> "inventory.reserved".equals(e.getTopic()))
                .hasSize(1);
    }

    @Test
    void release_marksReservationReleased_andWritesOutbox() {
        outboxRepository.deleteAll();
        reservationRepository.deleteAll();
        equipmentRepository.deleteAll();

        Equipment equipment = new Equipment();
        equipment.setModel("MAZ");
        equipment.setStatus("AVAILABLE");
        equipment = equipmentRepository.save(equipment);

        reservationService.reserveFromProcurementApproval(new Events.ProcurementApproved(200L, equipment.getId(), 1L));
        reservationService.releaseFromProcurementReject(new Events.ProcurementRejected(200L, equipment.getId(), 1L));

        Equipment updated = equipmentRepository.findById(equipment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualToIgnoringCase("AVAILABLE");

        assertThat(reservationRepository.findByRequestIdAndStatus(200L, "RELEASED")).isPresent();

        assertThat(outboxRepository.findAll())
                .filteredOn(e -> e.getAggregateId().equals(200L))
                .filteredOn(e -> "inventory.released".equals(e.getTopic()))
                .hasSize(1);
    }
}
