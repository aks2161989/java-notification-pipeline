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
    public CommandLineRunner runner(DatabaseSeederService seederService) {
        return args -> {
            log.info("Humanitarians AI Notification Pipeline initialized successfully.");
        };
    }
}
