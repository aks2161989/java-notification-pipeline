package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.adapter.email.EmailSink;
import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.EmailNotification;
import ai.humanitarians.pipeline.model.dto.HealthReport;
import ai.humanitarians.pipeline.model.dto.RetryReport;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class RecoveryServiceTest {

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

    @Autowired
    private PipelineHealthService healthService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reviewEventRepository.deleteAll();
        videoRepository.deleteAll();
        fellowRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    @DisplayName("Health monitor detects stranded SENDING notifications (>10 minutes)")
    void testDoctorDetectsStrandedSending() {
        Project project = projectRepository.save(new Project(1, "Madison", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(1, project, "John", "john@example.com", "active", "John"));
        VideoRecord video = videoRepository.save(new VideoRecord(1, fellow, "Test", "yt1", VideoStatus.QUEUED));

        // Stranded sending record from 20 minutes ago
        NotificationLog stranded = new NotificationLog(video, "john@example.com", NotificationDeliveryStatus.SENDING, "In flight");
        stranded.setSentOn(OffsetDateTime.now().minusMinutes(20));
        stranded = notificationRepository.save(stranded);

        HealthReport report = healthService.checkHealth();
        final Integer targetId = stranded.getNotificationId();
        assertTrue(report.getIssues().stream().anyMatch(i ->
                "STRANDED_JOB".equalsIgnoreCase(i.category()) && targetId.equals(i.recordId())
        ));
    }

    @Test
    @DisplayName("Health monitor flags permanently failed notifications that exhausted retries")
    void testDoctorDetectsExhaustedRetries() {
        Project project = projectRepository.save(new Project(2, "NeuroVEP", "internal"));
        Fellow fellow = fellowRepository.save(new Fellow(2, project, "Maya", "maya@example.com", "active", "Maya"));
        VideoRecord video = videoRepository.save(new VideoRecord(2, fellow, "Test 2", "yt2", VideoStatus.QUEUED));

        NotificationLog failedNotif = new NotificationLog(video, "maya@example.com", NotificationDeliveryStatus.FAILED, "Repeated SMTP timeout");
        failedNotif.setAttemptCount(3);
        failedNotif = notificationRepository.save(failedNotif);

        HealthReport report = healthService.checkHealth();
        assertFalse(report.isHealthy()); // CRITICAL issue makes pipeline unhealthy
        final Integer targetId = failedNotif.getNotificationId();
        assertTrue(report.getIssues().stream().anyMatch(i ->
                "RETRY_EXHAUSTED".equalsIgnoreCase(i.category()) && targetId.equals(i.recordId())
        ));
    }

    @Test
    @DisplayName("NotificationRetryService dispatches failed records and transitions them to SENT")
    void testRetryServiceSuccessfulRetry() {
        Project project = projectRepository.save(new Project(3, "Lyrical", "external"));
        Fellow fellow = fellowRepository.save(new Fellow(3, project, "Marcus", "marcus@example.com", "active", "Marcus"));
        VideoRecord video = videoRepository.save(new VideoRecord(3, fellow, "Test 3", "yt3", VideoStatus.QUEUED));

        NotificationLog failedNotif = new NotificationLog(video, "marcus@example.com", NotificationDeliveryStatus.FAILED, "Temporary timeout");
        failedNotif.setAttemptCount(1);
        failedNotif = notificationRepository.save(failedNotif);

        EmailSink mockSink = Mockito.mock(EmailSink.class);
        when(mockSink.deliver(any(EmailNotification.class))).thenReturn(true);

        PipelineProperties props = new PipelineProperties();
        props.setMode(PipelineMode.PROD);
        NotificationDispatcherService dispatcher = new NotificationDispatcherService(props, emailFormatter, List.of(mockSink));
        NotificationRetryService retryService = new NotificationRetryService(props, notificationRepository, dispatcher);

        RetryReport report = retryService.retryFailed(3, PipelineMode.PROD, null);
        assertEquals(1, report.getRetriedCount());
        assertEquals(1, report.getSucceededCount());
        verify(mockSink, times(1)).deliver(any(EmailNotification.class));

        // Verify database state after retry
        NotificationLog notifAfter = notificationRepository.findById(failedNotif.getNotificationId()).orElseThrow();
        assertEquals(NotificationDeliveryStatus.SENT, notifAfter.getDeliveryStatus());
        assertEquals(2, notifAfter.getAttemptCount());
        assertTrue(notifAfter.getReason().toLowerCase().contains("retry"));
    }
}
