package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.adapter.youtube.MockYouTubeAdapter;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.model.entity.*;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import ai.humanitarians.pipeline.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class SimulationFlowTest {

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
    private ReconciliationService reconciliationService;

    @Autowired
    private ai.humanitarians.pipeline.adapter.youtube.YouTubeAdapter youTubeAdapter;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reviewEventRepository.deleteAll();
        videoRepository.deleteAll();
        fellowRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Step 4: Publish simulation, pre-commit, dispatch, idempotency guard and reset")
    void testEndToEndSimulationAndReconciliationFlow() {
        // 1. Setup Project, Fellow and Video 99 in QUEUED state
        Project project = projectRepository.save(new Project(1, "Madison", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(1, project, "Jainil Shah", "jainil@test.org", "active", "Jainil Shah (Madison)"));

        VideoRecord v99 = new VideoRecord(99, fellow, "My Test Pipeline Video", "cGOYmLtt5kk", VideoStatus.QUEUED);
        v99.setYoutubeLink("https://youtu.be/cGOYmLtt5kk");
        videoRepository.save(v99);

        assertTrue(youTubeAdapter instanceof MockYouTubeAdapter);
        MockYouTubeAdapter mockYT = (MockYouTubeAdapter) youTubeAdapter;
        mockYT.simulateUnpublish("cGOYmLtt5kk");

        // 2. Initial run: video is not yet public -> skipped
        ReconciliationReport report1 = reconciliationService.runReconciliation(PipelineMode.DRY_RUN, null, true);
        assertEquals(1, report1.getVideosInspected());
        assertEquals(0, report1.getNewPublicationsDetected());
        assertEquals(0, report1.getNotificationsSent());

        // 3. Reviewer marks video public via simulation endpoint
        boolean published = mockYT.simulatePublish("cGOYmLtt5kk");
        assertTrue(published);
        assertTrue(mockYT.isVideoPublic("cGOYmLtt5kk"));

        // 4. Reconciliation runs in REDIRECTED mode: detects publication, pre-commits and dispatches
        ReconciliationReport report2 = reconciliationService.runReconciliation(PipelineMode.REDIRECTED, "test@test.org", true);
        assertEquals(1, report2.getVideosInspected());
        assertEquals(1, report2.getNewPublicationsDetected());
        assertEquals(1, report2.getNotificationsSent());

        // Verify Video 99 state updated to PUBLISHED
        VideoRecord updated = videoRepository.findById(99).orElseThrow();
        assertEquals(VideoStatus.PUBLISHED, updated.getCurrentStatus());
        assertNotNull(updated.getPublishedOn());

        // Verify notification log exists in DB
        var notifs = notificationRepository.findByVideo_VideoIdOrderByNotificationIdDesc(99);
        assertFalse(notifs.isEmpty());
        assertEquals("jainil@test.org", notifs.get(0).getRecipientEmail());

        // 5. Subsequent run: Idempotency Guard prevents duplicate dispatch
        // If video status is changed back to queued while keeping existing notification log, duplicate is prevented
        VideoRecord v99Reloaded = videoRepository.findById(99).orElseThrow();
        v99Reloaded.setCurrentStatus(VideoStatus.QUEUED);
        videoRepository.save(v99Reloaded);

        ReconciliationReport report3 = reconciliationService.runReconciliation(PipelineMode.DRY_RUN, null, true);
        assertEquals(1, report3.getDuplicatesPrevented());

        // 6. Reset for repeatable testing
        notificationRepository.deleteByVideoId(99);
        mockYT.simulateUnpublish("cGOYmLtt5kk");
        VideoRecord resetVideo = videoRepository.findById(99).orElseThrow();
        assertEquals(VideoStatus.QUEUED, resetVideo.getCurrentStatus());
        assertTrue(notificationRepository.findByVideo_VideoIdOrderByNotificationIdDesc(99).isEmpty());
    }
}
