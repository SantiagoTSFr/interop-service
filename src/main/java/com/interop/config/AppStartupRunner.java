package com.interop.config;

import com.interop.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the full search index from the DB on application startup,
 * so /search works immediately without requiring a prior /ingest call.
 */
@Component
public class AppStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AppStartupRunner.class);

    private final IngestService ingestService;

    public AppStartupRunner(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ingestService.loadFullIndex();
        } catch (Exception e) {
            log.warn("Could not load search index on startup (DB may not be ready): {}", e.getMessage());
        }
    }
}
