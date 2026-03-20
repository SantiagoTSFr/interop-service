package com.interop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * Strategy (auto-detected at runtime):
 *  1. If Docker is available → spin up a real postgres:16 container via Testcontainers.
 *     The container is initialised from db/init.sql (same script as production).
 *  2. If Docker is NOT available (e.g. Docker Desktop not started) → fall back to the
 *     local docker-compose DB on localhost:5432 with the standard interop credentials.
 *     In this case you must run `docker compose up -d` before running tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);

    // Lazily started — only if Docker is available
    private static final PostgreSQLContainer<?> postgres;

    static {
        PostgreSQLContainer<?> container = null;
        try {
            // Check if Docker daemon is reachable before trying to start a container
            DockerClientFactory.instance().client().infoCmd().exec();
            container = new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("interop")
                    .withUsername("interop")
                    .withPassword("interop")
                    .withInitScript("db/init.sql");
            container.start();
            log.info("Testcontainers: postgres:16 started on {}", container.getJdbcUrl());
        } catch (Exception e) {
            log.warn("Docker not available ({}). Falling back to local docker-compose DB " +
                    "at localhost:5432. Make sure `docker compose up -d` has been run.", e.getMessage());
            container = null;
        }
        postgres = container;
    }

    @DynamicPropertySource
    static void overrideDataSource(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            // Testcontainers path
            registry.add("spring.datasource.url",      postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        } else {
            // Fallback: local docker-compose DB
            registry.add("spring.datasource.url",
                    () -> "jdbc:postgresql://localhost:5432/interop");
            registry.add("spring.datasource.username", () -> "interop");
            registry.add("spring.datasource.password", () -> "interop");
        }
    }
}
