package ai.humanitarians.pipeline.model.dto;

public record VideoProcessingResult(
        Integer videoId,
        String youtubeVideoId,
        String actionTaken,
        String fellowEmail,
        Integer notificationId,
        String detail
) {}
