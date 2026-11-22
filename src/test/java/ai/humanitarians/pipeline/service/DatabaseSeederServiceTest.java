package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.model.dto.MetricsSummary;
import ai.humanitarians.pipeline.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseSeederServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private FellowRepository fellowRepository;

    @Autowired
    private VideoRecordRepository videoRepository;

    @Autowired
    private NotificationLogRepository notificationRepository;

    @Autowired
    private ReviewEventRepository reviewEventRepository;

    @Autowired
    private PMRepository pmRepository;

    @Autowired
    private DatabaseSeederService seederService;

    @Autowired
    private PipelineHealthService healthService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reviewEventRepository.deleteAll();
        videoRepository.deleteAll();
        fellowRepository.deleteAll();
        pmRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("Seeder populates complete synthetic scenario matrix")
    void testSyntheticScenarioSeeding() {
        Map<String, Integer> counts = seederService.seedSyntheticScenarios();
        assertTrue(counts.get("projects") >= 4);
        assertTrue(counts.get("fellows") >= 5);
        assertTrue(counts.get("videos") >= 5);

        MetricsSummary metrics = healthService.getMetrics();
        assertTrue(metrics.totalProjects() >= 4);
        assertTrue(metrics.totalFellows() >= 5);
        assertTrue(metrics.totalVideos() >= 5);
        assertTrue(metrics.sentNotifications() >= 1);
    }

    @Test
    @DisplayName("Seeder ingests YouTube_Pipeline_Tracker.xlsx if present on disk")
    void testExcelIngestionIfPresent() {
        Path xlsxPath = Path.of("data/YouTube_Pipeline_Tracker.xlsx");
        if (java.nio.file.Files.exists(xlsxPath)) {
            Map<String, Integer> counts = seederService.seedFromExcel(xlsxPath);
            assertTrue(counts.get("projects") > 0);
            assertTrue(counts.get("fellows") > 0);
            assertTrue(counts.get("videos") > 0);
        }
    }
}
