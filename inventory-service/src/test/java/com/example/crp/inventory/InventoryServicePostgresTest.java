package com.example.crp.inventory;

import com.example.crp.inventory.domain.Equipment;
import com.example.crp.inventory.repo.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,com.example.crp.security.JwtAudienceAutoConfiguration",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import(com.example.crp.inventory.config.TestSecurityConfig.class)
class InventoryServicePostgresTest {

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
    private EquipmentRepository repository;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void repositoryPersistsEquipmentInPostgres() {
        Equipment equipment = new Equipment();
        equipment.setModel("Dell Latitude");
        equipment.setStatus("AVAILABLE");

        Equipment saved = repository.save(equipment);

        assertThat(saved.getId()).isNotNull();

        Equipment found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getModel()).isEqualTo("Dell Latitude");
        assertThat(found.getStatus()).isEqualTo("AVAILABLE");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createEndpointPersistsToPostgres() throws Exception {
        Equipment payload = new Equipment();
        payload.setModel("HP ProBook");
        payload.setStatus("AVAILABLE");

        mockMvc.perform(post("/equipment")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.model").value("HP ProBook"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        List<Equipment> all = repository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.stream().anyMatch(e -> "HP ProBook".equals(e.getModel()))).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listEndpointReturnsDataFromPostgres() throws Exception {
        Equipment equipment = new Equipment();
        equipment.setModel("Lenovo ThinkPad");
        equipment.setStatus("AVAILABLE");
        repository.save(equipment);

        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].model").value("Lenovo ThinkPad"));
    }
}
