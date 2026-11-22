package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.model.dto.MetricsSummary;
import ai.humanitarians.pipeline.model.entity.*;
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

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("Sync fellows from CSV with unassigned projects and active/inactive reconciliation")
    void testFellowCsvSyncAndReconciliation() {
        String csv1 = """
                Name,Email,Project
                Dev Shah,devshahofficial001@gmail.com,IACON / Optimus / RL Environment
                Soumya Nayak,soumyanayak.neu@gmail.com,
                """;

        Map<String, Integer> counts1 = seederService.syncFellowsFromCsv(csv1);
        assertEquals(2, counts1.get("added"));
        assertEquals(2, counts1.get("total_active"));

        Fellow dev = fellowRepository.findByEmail("devshahofficial001@gmail.com").orElseThrow();
        assertEquals("active", dev.getStatus());
        assertEquals("IACON / Optimus / RL Environment", dev.getProject().getProjectName());

        Fellow soumya = fellowRepository.findByEmail("soumyanayak.neu@gmail.com").orElseThrow();
        assertEquals("active", soumya.getStatus());
        assertEquals("General / Unassigned", soumya.getProject().getProjectName());

        // Now test removal / deactivation: CSV2 removes Soumya and adds Rishabh
        String csv2 = """
                Name,Email,Project
                Dev Shah,devshahofficial001@gmail.com,IACON / Optimus / RL Environment
                Rishabh Madani,mrishabh67.ai@gmail.com,
                """;

        Map<String, Integer> counts2 = seederService.syncFellowsFromCsv(csv2);
        assertEquals(1, counts2.get("added"));
        assertEquals(1, counts2.get("updated"));
        assertEquals(1, counts2.get("deactivated"));
        assertEquals(2, counts2.get("total_active"));

        Fellow soumyaUpdated = fellowRepository.findByEmail("soumyanayak.neu@gmail.com").orElseThrow();
        assertEquals("inactive", soumyaUpdated.getStatus());

        Fellow rishabh = fellowRepository.findByEmail("mrishabh67.ai@gmail.com").orElseThrow();
        assertEquals("active", rishabh.getStatus());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    @DisplayName("VideoSummary DTO maps entity fields cleanly")
    void testVideoSummaryDtoMapping() {
        Project proj = projectRepository.save(new Project(101, "Test Project", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(201, proj, "Test Fellow", "fellow@test.org", "active", "Test Fellow"));
        ai.humanitarians.pipeline.model.entity.VideoRecord video = new ai.humanitarians.pipeline.model.entity.VideoRecord(
                301, fellow, "Intro to AI", "abc123xyz89", ai.humanitarians.pipeline.model.enums.VideoStatus.QUEUED
        );
        video.setYoutubeLink("https://youtu.be/abc123xyz89");
        video.setDriveLink("https://drive.google.com/file/d/test-drive-id/view");
        video.setSubmittedOn(java.time.OffsetDateTime.now());
        video = videoRepository.save(video);

        ai.humanitarians.pipeline.model.dto.VideoSummary summary = ai.humanitarians.pipeline.model.dto.VideoSummary.from(video);
        assertEquals(301, summary.videoId());
        assertEquals("Test Fellow", summary.fellowName());
        assertEquals("fellow@test.org", summary.fellowEmail());
        assertEquals("Test Project", summary.projectName());
        assertEquals("Intro to AI", summary.title());
        assertEquals("abc123xyz89", summary.youtubeVideoId());
        assertEquals("queued", summary.currentStatus());
    }
}
