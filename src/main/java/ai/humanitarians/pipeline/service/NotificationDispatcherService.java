package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.adapter.email.ConsoleEmailSink;
import ai.humanitarians.pipeline.adapter.email.EmailSink;
import ai.humanitarians.pipeline.adapter.email.FileOutboxEmailSink;
import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.DispatchResult;
import ai.humanitarians.pipeline.model.dto.EmailNotification;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcherService.class);

    private final PipelineProperties properties;
    private final EmailFormatter emailFormatter;
    private final List<EmailSink> sinks;

    @org.springframework.beans.factory.annotation.Autowired
    public NotificationDispatcherService(PipelineProperties properties, EmailFormatter emailFormatter) {
        this.properties = properties;
        this.emailFormatter = emailFormatter;
        this.sinks = new ArrayList<>();
        this.sinks.add(new ConsoleEmailSink());
        this.sinks.add(new FileOutboxEmailSink(Path.of(properties.getOutboxDir())));
    }

    public NotificationDispatcherService(
            PipelineProperties properties,
            EmailFormatter emailFormatter,
            List<EmailSink> customSinks
    ) {
        this.properties = properties;
        this.emailFormatter = emailFormatter;
        this.sinks = customSinks != null ? customSinks : new ArrayList<>();
    }

    public DispatchResult dispatch(
            Integer videoId,
            Integer fellowId,
            String fellowName,
            String fellowEmail,
            String fellowStatus,
            String videoTitle,
            String youtubeVideoId,
            String projectName,
            PipelineMode mode,
            String redirectTo
    ) {
        PipelineMode effMode = mode != null ? mode : properties.getMode();

        // 1. Inactive fellow check
        if (fellowStatus != null && !"active".equalsIgnoreCase(fellowStatus.trim())) {
            return new DispatchResult(
                    false,
                    NotificationDeliveryStatus.SKIPPED,
                    fellowEmail,
                    String.format("Fellow status is '%s'; notification skipped", fellowStatus),
                    null
            );
        }

        // 2. Email hygiene check
        if (fellowEmail == null || fellowEmail.isBlank() || !fellowEmail.contains("@")) {
            return new DispatchResult(
                    false,
                    NotificationDeliveryStatus.FAILED,
                    fellowEmail != null ? fellowEmail : "EMPTY",
                    "Invalid fellow email address",
                    null
            );
        }

        // 3. Canary / Cohort filter
        if (effMode == PipelineMode.COHORT) {
            if (!properties.getCohortFellowIds().contains(fellowId)) {
                return new DispatchResult(
                        false,
                        NotificationDeliveryStatus.SKIPPED,
                        fellowEmail,
                        String.format("Fellow ID %d not in canary cohort %s", fellowId, properties.getCohortFellowIds()),
                        null
                );
            }
        }

        // 4. Render email
        EmailNotification notification = emailFormatter.render(
                videoId, fellowId, fellowName, fellowEmail, videoTitle,
                youtubeVideoId, projectName, effMode, redirectTo
        );

        // 5. DRY_RUN mode
        if (effMode == PipelineMode.DRY_RUN) {
            for (EmailSink sink : sinks) {
                sink.deliver(notification);
            }
            return new DispatchResult(
                    true,
                    NotificationDeliveryStatus.SKIPPED,
                    notification.recipientEmail(),
                    "Dry-run mode evaluated successfully (send bypassed)",
                    null
            );
        }

        // 6. Live / Redirected / Cohort send
        try {
            for (EmailSink sink : sinks) {
                sink.deliver(notification);
            }
            return new DispatchResult(
                    true,
                    NotificationDeliveryStatus.SENT,
                    notification.recipientEmail(),
                    "Email successfully dispatched",
                    OffsetDateTime.now()
            );
        } catch (Exception e) {
            log.error("Email dispatch failed for video {}: {}", videoId, e.getMessage(), e);
            return new DispatchResult(
                    false,
                    NotificationDeliveryStatus.FAILED,
                    notification.recipientEmail(),
                    "Delivery exception: " + e.getMessage(),
                    null
            );
        }
    }
}
