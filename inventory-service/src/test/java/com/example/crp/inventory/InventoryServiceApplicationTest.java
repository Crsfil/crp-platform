package com.example.crp.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.crp.inventory.config.TestcontainersDisabler;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration,com.example.crp.security.JwtAudienceAutoConfiguration",
        "outbox.publisher.enabled=false",
        "spring.main.allow-bean-definition-overriding=true"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@ExtendWith(TestcontainersDisabler.class)
@Import(com.example.crp.inventory.config.TestSecurityConfig.class)
class InventoryServiceApplicationTest {

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

    @Test
    void contextLoads() {
        // just verify that the Spring context starts
    }

    @Test
    void mainMethodRuns() {
        String[] args = {
                "--spring.profiles.active=test",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--DB_URL=" + postgres.getJdbcUrl(),
                "--DB_USERNAME=" + postgres.getUsername(),
                "--DB_PASSWORD=" + postgres.getPassword()
        };

        assertThatCode(() -> InventoryServiceApplication.main(args))
                .doesNotThrowAnyException();
    }
}
