package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.adapter.youtube.YouTubeAdapter;
import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.DispatchResult;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.model.dto.VideoProcessingResult;
import ai.humanitarians.pipeline.model.entity.Fellow;
import ai.humanitarians.pipeline.model.entity.NotificationLog;
import ai.humanitarians.pipeline.model.entity.ReviewEvent;
import ai.humanitarians.pipeline.model.entity.VideoRecord;
import ai.humanitarians.pipeline.model.enums.EventType;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import ai.humanitarians.pipeline.repository.NotificationLogRepository;
import ai.humanitarians.pipeline.repository.ReviewEventRepository;
import ai.humanitarians.pipeline.repository.VideoRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PipelineProperties properties;
    private final VideoRecordRepository videoRepository;
    private final NotificationLogRepository notificationRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final YouTubeAdapter youTubeAdapter;
    private final NotificationDispatcherService dispatcher;
    private final ExecutionLockService lockService;

    public ReconciliationService(
            PipelineProperties properties,
            VideoRecordRepository videoRepository,
            NotificationLogRepository notificationRepository,
            ReviewEventRepository reviewEventRepository,
            YouTubeAdapter youTubeAdapter,
            NotificationDispatcherService dispatcher,
            ExecutionLockService lockService
    ) {
        this.properties = properties;
        this.videoRepository = videoRepository;
        this.notificationRepository = notificationRepository;
        this.reviewEventRepository = reviewEventRepository;
        this.youTubeAdapter = youTubeAdapter;
        this.dispatcher = dispatcher;
        this.lockService = lockService;
    }

    public ReconciliationReport runReconciliation() {
        return runReconciliation(null, null, false);
    }

    public ReconciliationReport runReconciliation(PipelineMode mode, String redirectTo, boolean bypassLock) {
        PipelineMode effMode = mode != null ? mode : properties.getMode();
        ReconciliationReport report = new ReconciliationReport(effMode);

        ExecutionLockService.LockHandle lockHandle = null;
        if (!bypassLock) {
            try {
                lockHandle = lockService.acquire();
            } catch (LockAcquisitionException e) {
                log.warn("Aborting execution run: {}", e.getMessage());
                report.getProcessedItems().add(new VideoProcessingResult(
                        0, "N/A", "ABORTED_LOCK_COLLISION", null, null, e.getMessage()
                ));
                return report;
            }
        }

        try {
            List<VideoRecord> queuedVideos = videoRepository.findByCurrentStatus(VideoStatus.QUEUED);
            report.setVideosInspected(queuedVideos.size());

            for (VideoRecord video : queuedVideos) {
                VideoProcessingResult res = processSingleVideo(video, effMode, redirectTo);
                report.getProcessedItems().add(res);

                switch (res.actionTaken()) {
                    case "PUBLISHED_NOTIFIED" -> {
                        report.incrementNewPublications();
                        report.incrementNotificationsSent();
                    }
                    case "DRY_RUN" -> {
                        report.incrementNewPublications();
                        report.incrementNotificationsSkipped();
                    }
                    case "SKIPPED_DUPLICATE" -> report.incrementDuplicatesPrevented();
                    case "SKIPPED_CANARY", "SKIPPED_INACTIVE" -> report.incrementNotificationsSkipped();
                    case "FAILED" -> report.incrementFailures();
                    default -> {}
                }
            }
        } finally {
            if (lockHandle != null) {
                lockHandle.release();
            }
        }

        return report;
    }

    @Transactional
    public VideoProcessingResult processSingleVideo(VideoRecord video, PipelineMode mode, String redirectTo) {
        String ytId = video.getYoutubeVideoId();
        if (ytId == null || ytId.isBlank()) {
            return new VideoProcessingResult(
                    video.getVideoId(), "NONE", "FAILED", null, null,
                    "Missing YouTube Video ID in video record"
            );
        }

        // 1. YouTube publication status check
        boolean isPublic = youTubeAdapter.isVideoPublic(ytId);
        if (!isPublic) {
            return new VideoProcessingResult(
                    video.getVideoId(), ytId, "SKIPPED_NOT_PUBLIC", null, null,
                    "Video is still in Queue (draft/unlisted/private)"
            );
        }

        // 2. Idempotency Guard: check if notification exists with 'sending' or 'sent'
        Optional<NotificationLog> existingNotif = notificationRepository.findActiveOrSent(video.getVideoId());
        if (existingNotif.isPresent()) {
            NotificationLog notif = existingNotif.get();
            return new VideoProcessingResult(
                    video.getVideoId(), ytId, "SKIPPED_DUPLICATE", notif.getRecipientEmail(),
                    notif.getNotificationId(),
                    String.format("Notification already recorded with delivery_status='%s'", notif.getDeliveryStatus().getValue())
            );
        }

        // 3. Resolve fellow & project
        Fellow fellow = video.getFellow();
        if (fellow == null) {
            return new VideoProcessingResult(
                    video.getVideoId(), ytId, "FAILED", null, null,
                    "Video record is not linked to any Fellow"
            );
        }

        String projectName = (fellow.getProject() != null && fellow.getProject().getProjectName() != null)
                ? fellow.getProject().getProjectName()
                : "Humanitarians AI";

        // 4. Two-Phase State Machine
        // STEP 4A: Pre-commit SENDING record to DB before network call
        OffsetDateTime now = OffsetDateTime.now();
        NotificationLog notif = new NotificationLog(
                video, fellow.getEmail(), NotificationDeliveryStatus.SENDING,
                "Video became public on YouTube (" + ytId + ")"
        );
        notif = notificationRepository.saveAndFlush(notif);

        video.setCurrentStatus(VideoStatus.PUBLISHED);
        video.setPublishedOn(now);
        videoRepository.saveAndFlush(video);

        ReviewEvent event = new ReviewEvent(
                video, EventType.PUBLISHED, "youtube_poller",
                "Auto-detected publication on YouTube (ID: " + ytId + ")", now
        );
        reviewEventRepository.saveAndFlush(event);

        // STEP 4B: Dispatch email notification
        String videoTitle = (video.getTitle() != null && !video.getTitle().isBlank())
                ? video.getTitle()
                : "Video " + video.getVideoId();

        DispatchResult dispatchResult = dispatcher.dispatch(
                video.getVideoId(), fellow.getFellowId(), fellow.getFullName(), fellow.getEmail(),
                fellow.getStatus(), videoTitle, ytId, projectName, mode, redirectTo
        );

        // STEP 4C: Update notification state to SENT, FAILED, or SKIPPED
        notif.setDeliveryStatus(dispatchResult.deliveryStatus());
        notif.setReason(dispatchResult.reason());
        if (dispatchResult.deliveryStatus() == NotificationDeliveryStatus.SENT) {
            notif.setSentOn(now);
        }
        notificationRepository.saveAndFlush(notif);

        String action;
        if (mode == PipelineMode.DRY_RUN) {
            action = "DRY_RUN";
        } else if (dispatchResult.deliveryStatus() == NotificationDeliveryStatus.SENT) {
            action = "PUBLISHED_NOTIFIED";
        } else if (dispatchResult.deliveryStatus() == NotificationDeliveryStatus.SKIPPED) {
            action = dispatchResult.reason().toLowerCase().contains("cohort") ? "SKIPPED_CANARY" : "SKIPPED_INACTIVE";
        } else {
            action = "FAILED";
        }

        return new VideoProcessingResult(
                video.getVideoId(), ytId, action, dispatchResult.actualRecipient(),
                notif.getNotificationId(), dispatchResult.reason()
        );
    }
}
