package ai.humanitarians.pipeline.model.dto;

import java.time.OffsetDateTime;

public record DriveFileMetadata(
        String fileId,
        String name,
        String mimeType,
        long sizeBytes,
        OffsetDateTime createdTime,
        String folderPath,
        String submitterEmail
) {}
