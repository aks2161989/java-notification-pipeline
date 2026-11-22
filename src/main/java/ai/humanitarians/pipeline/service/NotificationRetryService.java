package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.DispatchResult;
import ai.humanitarians.pipeline.model.dto.RetryReport;
import ai.humanitarians.pipeline.model.entity.Fellow;
import ai.humanitarians.pipeline.model.entity.NotificationLog;
import ai.humanitarians.pipeline.model.entity.VideoRecord;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NotificationRetryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryService.class);

    private final PipelineProperties properties;
    private final NotificationLogRepository notificationRepository;
    private final NotificationDispatcherService dispatcher;

    public NotificationRetryService(
            PipelineProperties properties,
            NotificationLogRepository notificationRepository,
            NotificationDispatcherService dispatcher
    ) {
        this.properties = properties;
        this.notificationRepository = notificationRepository;
        this.dispatcher = dispatcher;
    }

    public RetryReport retryFailed() {
        return retryFailed(properties.getMaxRetryAttempts(), properties.getMode(), null);
    }

    @Transactional
    public RetryReport retryFailed(int maxAttempts, PipelineMode mode, String redirectTo) {
        PipelineMode effMode = mode != null ? mode : properties.getMode();
        RetryReport report = new RetryReport();

        List<NotificationLog> retryable = notificationRepository.findRetryableFailed(
                NotificationDeliveryStatus.FAILED, maxAttempts
        );
        report.setRetriedCount(retryable.size());

        for (NotificationLog notif : retryable) {
            VideoRecord video = notif.getVideo();
            if (video == null) continue;
            Fellow fellow = video.getFellow();
            if (fellow == null) continue;

            String projectName = (fellow.getProject() != null && fellow.getProject().getProjectName() != null)
                    ? fellow.getProject().getProjectName()
                    : "Humanitarians AI";

            String videoTitle = (video.getTitle() != null && !video.getTitle().isBlank())
                    ? video.getTitle()
                    : "Video " + video.getVideoId();

            String ytId = video.getYoutubeVideoId() != null ? video.getYoutubeVideoId() : "UNKNOWN";

            DispatchResult res = dispatcher.dispatch(
                    video.getVideoId(), fellow.getFellowId(), fellow.getFullName(), fellow.getEmail(),
                    fellow.getStatus(), videoTitle, ytId, projectName, effMode, redirectTo
            );

            notif.setAttemptCount(notif.getAttemptCount() + 1);

            if (res.success() && res.deliveryStatus() == NotificationDeliveryStatus.SENT) {
                notif.setDeliveryStatus(NotificationDeliveryStatus.SENT);
                notif.setReason("Successfully delivered on retry");
                notif.setSentOn(OffsetDateTime.now());
                notificationRepository.save(notif);

                report.setSucceededCount(report.getSucceededCount() + 1);
                report.getDetails().add(String.format("Notification #%d (Video #%d) succeeded on retry.",
                        notif.getNotificationId(), video.getVideoId()));
            } else {
                notif.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                notif.setReason("Retry failed: " + res.reason());
                notificationRepository.save(notif);

                if (notif.getAttemptCount() >= maxAttempts) {
                    report.setExhaustedCount(report.getExhaustedCount() + 1);
                    report.getDetails().add(String.format("Notification #%d (Video #%d) exhausted all %d attempts.",
                            notif.getNotificationId(), video.getVideoId(), maxAttempts));
                } else {
                    report.getDetails().add(String.format("Notification #%d failed attempt %d/%d.",
                            notif.getNotificationId(), notif.getAttemptCount(), maxAttempts));
                }
            }
        }

        return report;
    }
}
