package ai.humanitarians.pipeline.model.dto;

import ai.humanitarians.pipeline.model.enums.PipelineMode;
import java.time.OffsetDateTime;
import java.util.Map;

public record EmailNotification(
        Integer videoId,
        Integer fellowId,
        String fellowName,
        String recipientEmail,
        String intendedRecipient,
        String videoTitle,
        String youtubeVideoId,
        String youtubeUrl,
        String projectName,
        String subject,
        String bodyText,
        String bodyHtml,
        PipelineMode mode,
        Map<String, String> headers,
        OffsetDateTime createdAt
) {}
