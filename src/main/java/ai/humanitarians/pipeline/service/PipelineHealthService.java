package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.model.dto.HealthReport;
import ai.humanitarians.pipeline.model.dto.MetricsSummary;
import ai.humanitarians.pipeline.model.entity.NotificationLog;
import ai.humanitarians.pipeline.model.entity.VideoRecord;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import ai.humanitarians.pipeline.repository.FellowRepository;
import ai.humanitarians.pipeline.repository.NotificationLogRepository;
import ai.humanitarians.pipeline.repository.ProjectRepository;
import ai.humanitarians.pipeline.repository.VideoRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PipelineHealthService {

    private final ProjectRepository projectRepository;
    private final FellowRepository fellowRepository;
    private final VideoRecordRepository videoRepository;
    private final NotificationLogRepository notificationRepository;

    public PipelineHealthService(
            ProjectRepository projectRepository,
            FellowRepository fellowRepository,
            VideoRecordRepository videoRepository,
            NotificationLogRepository notificationRepository
    ) {
        this.projectRepository = projectRepository;
        this.fellowRepository = fellowRepository;
        this.videoRepository = videoRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public HealthReport checkHealth() {
        List<HealthReport.HealthIssue> issues = new ArrayList<>();
        Map<String, Object> stats = new HashMap<>();

        MetricsSummary metrics = getMetrics();
        stats.put("total_projects", metrics.totalProjects());
        stats.put("total_fellows", metrics.totalFellows());
        stats.put("total_videos", metrics.totalVideos());
        stats.put("queued_videos", metrics.queuedVideos());
        stats.put("published_videos", metrics.publishedVideos());
        stats.put("sent_notifications", metrics.sentNotifications());
        stats.put("failed_notifications", metrics.failedNotifications());

        // 1. Check stranded SENDING notifications (>10 minutes)
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(10);
        List<NotificationLog> stranded = notificationRepository.findStrandedSending(cutoff);
        for (NotificationLog n : stranded) {
            issues.add(new HealthReport.HealthIssue(
                    "WARNING",
                    "STRANDED_JOB",
                    String.format("Notification #%d for Video #%d has been stranded in SENDING state.",
                            n.getNotificationId(), n.getVideo().getVideoId()),
                    n.getNotificationId()
            ));
        }

        // 2. Check for exhausted retries (failed >= 3 times) and data quality anomalies
        List<VideoRecord> allVideos = videoRepository.findAllWithFellow();
        for (VideoRecord v : allVideos) {
            List<NotificationLog> notifs = notificationRepository.findByVideo_VideoIdOrderByNotificationIdDesc(v.getVideoId());
            for (NotificationLog n : notifs) {
                if (n.getDeliveryStatus() == NotificationDeliveryStatus.FAILED && n.getAttemptCount() >= 3) {
                    issues.add(new HealthReport.HealthIssue(
                            "CRITICAL",
                            "RETRY_EXHAUSTED",
                            String.format("Notification #%d for Video #%d (%s) exhausted 3 delivery attempts. Reason: %s",
                                    n.getNotificationId(), v.getVideoId(), v.getTitle(), n.getReason()),
                            n.getNotificationId()
                    ));
                }
            }

            // Data quality: missing fellow
            if (v.getFellow() == null) {
                issues.add(new HealthReport.HealthIssue(
                        "WARNING",
                        "DATA_QUALITY",
                        String.format("Video #%d ('%s') has no mapped Fellow ID.", v.getVideoId(), v.getTitle()),
                        v.getVideoId()
                ));
            } else if (v.getFellow().getEmail() == null || !v.getFellow().getEmail().contains("@")) {
                issues.add(new HealthReport.HealthIssue(
                        "CRITICAL",
                        "DATA_QUALITY",
                        String.format("Fellow #%d (%s) has invalid/missing email.",
                                v.getFellow().getFellowId(), v.getFellow().getFullName()),
                        v.getFellow().getFellowId()
                ));
            }
        }

        boolean isHealthy = issues.stream().noneMatch(i -> "CRITICAL".equalsIgnoreCase(i.severity()));
        return new HealthReport(isHealthy, issues, stats);
    }

    @Transactional(readOnly = true)
    public MetricsSummary getMetrics() {
        return new MetricsSummary(
                projectRepository.count(),
                fellowRepository.count(),
                videoRepository.count(),
                videoRepository.countByCurrentStatus(VideoStatus.QUEUED),
                videoRepository.countByCurrentStatus(VideoStatus.PUBLISHED),
                notificationRepository.countByDeliveryStatus(NotificationDeliveryStatus.SENT),
                notificationRepository.countByDeliveryStatus(NotificationDeliveryStatus.FAILED)
        );
    }
}
