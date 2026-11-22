package ai.humanitarians.pipeline.model.dto;

public record MetricsSummary(
        long totalProjects,
        long totalFellows,
        long totalVideos,
        long queuedVideos,
        long publishedVideos,
        long sentNotifications,
        long failedNotifications
) {}
