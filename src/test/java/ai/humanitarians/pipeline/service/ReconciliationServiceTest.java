package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.adapter.email.EmailSink;
import ai.humanitarians.pipeline.adapter.youtube.MockYouTubeAdapter;
import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.EmailNotification;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import ai.humanitarians.pipeline.model.entity.Fellow;
import ai.humanitarians.pipeline.model.entity.NotificationLog;
import ai.humanitarians.pipeline.model.entity.Project;
import ai.humanitarians.pipeline.model.entity.VideoRecord;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import ai.humanitarians.pipeline.repository.FellowRepository;
import ai.humanitarians.pipeline.repository.NotificationLogRepository;
import ai.humanitarians.pipeline.repository.ProjectRepository;
import ai.humanitarians.pipeline.repository.ReviewEventRepository;
import ai.humanitarians.pipeline.repository.VideoRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationServiceTest {

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
    private EmailFormatter emailFormatter;

    @TempDir
    Path tempDir;

    private PipelineProperties properties;
    private MockYouTubeAdapter mockYouTubeAdapter;
    private EmailSink mockSink;
    private NotificationDispatcherService dispatcher;
    private ExecutionLockService lockService;
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reviewEventRepository.deleteAll();
        videoRepository.deleteAll();
        fellowRepository.deleteAll();
        projectRepository.deleteAll();

        properties = new PipelineProperties();
        properties.setMode(PipelineMode.PROD);
        properties.setLockFilePath(tempDir.resolve(".reconcile.lock").toString());
        properties.setOutboxDir(tempDir.resolve("outbox").toString());

        mockYouTubeAdapter = new MockYouTubeAdapter(tempDir.resolve(".yt_state.json"));
        mockSink = Mockito.mock(EmailSink.class);
        when(mockSink.deliver(any(EmailNotification.class))).thenReturn(true);

        dispatcher = new NotificationDispatcherService(properties, emailFormatter, List.of(mockSink));
        lockService = new ExecutionLockService(properties);

        reconciliationService = new ReconciliationService(
                properties, videoRepository, notificationRepository,
                reviewEventRepository, mockYouTubeAdapter, dispatcher, lockService
        );
    }

    @Test
    @DisplayName("Two-phase commit and strict idempotency guard preventing duplicate notification delivery")
    void testIdempotencyStateMachineAndDuplicatePrevention() {
        // Seed test project, fellow, and video in queued state
        Project project = projectRepository.save(new Project(1, "Madison", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(1, project, "Jainil Shah", "shah.jainil@northeastern.edu", "active", "Jainil Shah"));
        VideoRecord video = videoRepository.save(new VideoRecord(10, fellow, "Madison Architecture", "yt_test_id", VideoStatus.QUEUED));

        // Configure mock YouTube: video is public
        mockYouTubeAdapter.registerVideo(new YouTubeVideoMetadata(
                "yt_test_id", "Madison Architecture", "public", "processed", null, null, "Queue"
        ));

        // First Run: Video is public -> should transition to PUBLISHED and dispatch 1 notification
        ReconciliationReport report1 = reconciliationService.runReconciliation(PipelineMode.PROD, null, false);
        assertEquals(1, report1.getNewPublicationsDetected());
        assertEquals(1, report1.getNotificationsSent());
        assertEquals(0, report1.getDuplicatesPrevented());
        verify(mockSink, times(1)).deliver(any(EmailNotification.class));

        // Verify database state after first run
        VideoRecord videoAfter = videoRepository.findById(10).orElseThrow();
        assertEquals(VideoStatus.PUBLISHED, videoAfter.getCurrentStatus());
        assertNotNull(videoAfter.getPublishedOn());

        List<NotificationLog> notifs = notificationRepository.findByVideo_VideoIdOrderByNotificationIdDesc(10);
        assertEquals(1, notifs.size());
        assertEquals(NotificationDeliveryStatus.SENT, notifs.get(0).getDeliveryStatus());

        // Second Run (simulating next scheduled cron trigger)
        // Reset video status to queued to simulate duplicate evaluation
        videoAfter.setCurrentStatus(VideoStatus.QUEUED);
        videoRepository.saveAndFlush(videoAfter);

        ReconciliationReport report2 = reconciliationService.runReconciliation(PipelineMode.PROD, null, false);
        assertEquals(1, report2.getDuplicatesPrevented());
        assertEquals(0, report2.getNotificationsSent());
        // Crucial assertion: email sink was NOT called again!
        verify(mockSink, times(1)).deliver(any(EmailNotification.class));
    }

    @Test
    @DisplayName("Email failure updates state to FAILED without crashing the pipeline")
    void testFailureHandlingAndRetryState() {
        Project project = projectRepository.save(new Project(2, "NeuroVEP", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(2, project, "Maya Lin", "m.lin@northeastern.edu", "active", "Maya Lin"));
        VideoRecord video = videoRepository.save(new VideoRecord(20, fellow, "NeuroVEP Analysis", "yt_fail_test", VideoStatus.QUEUED));

        mockYouTubeAdapter.registerVideo(new YouTubeVideoMetadata(
                "yt_fail_test", "NeuroVEP Analysis", "public", "processed", null, null, "Queue"
        ));

        // Failing sink simulating SMTP connection drop
        EmailSink failingSink = Mockito.mock(EmailSink.class);
        when(failingSink.deliver(any(EmailNotification.class)))
                .thenThrow(new RuntimeException("SMTP relay connection timeout"));

        NotificationDispatcherService failingDispatcher = new NotificationDispatcherService(
                properties, emailFormatter, List.of(failingSink)
        );
        ReconciliationService failingService = new ReconciliationService(
                properties, videoRepository, notificationRepository,
                reviewEventRepository, mockYouTubeAdapter, failingDispatcher, lockService
        );

        ReconciliationReport report = failingService.runReconciliation(PipelineMode.PROD, null, false);
        assertEquals(1, report.getFailures());
        assertEquals(0, report.getNotificationsSent());

        // Verify DB recorded failure reason rather than silently dropping
        List<NotificationLog> notifs = notificationRepository.findByVideo_VideoIdOrderByNotificationIdDesc(20);
        assertEquals(1, notifs.size());
        assertEquals(NotificationDeliveryStatus.FAILED, notifs.get(0).getDeliveryStatus());
        assertTrue(notifs.get(0).getReason().contains("SMTP relay connection timeout"));
    }
}
