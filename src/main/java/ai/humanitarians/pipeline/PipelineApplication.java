package ai.humanitarians.pipeline;

import ai.humanitarians.pipeline.service.DatabaseSeederService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PipelineApplication {

    private static final Logger log = LoggerFactory.getLogger(PipelineApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PipelineApplication.class, args);
    }

    @Bean
    public CommandLineRunner runner(
            DatabaseSeederService seederService,
            ai.humanitarians.pipeline.config.PipelineProperties properties
    ) {
        return args -> {
            log.info("Humanitarians AI Notification Pipeline initialized successfully.");
            if (properties.isAutoSeedOnStartup()) {
                try {
                    log.info("Auto-syncing tracker workbook (videos & fellows) from Google Sheet on startup...");
                    seederService.syncFromGoogleSheetWorkbook();
                } catch (Exception e) {
                    log.warn("Auto-sync tracker workbook on startup encountered an issue: {}. Attempting fellow roster fallback...", e.getMessage());
                    try {
                        seederService.syncFellowsFromGoogleSheet();
                    } catch (Exception ex) {
                        log.warn("Auto-sync fellow roster fallback also encountered an issue: {}", ex.getMessage());
                    }
                }
            }
        };
    }
}
