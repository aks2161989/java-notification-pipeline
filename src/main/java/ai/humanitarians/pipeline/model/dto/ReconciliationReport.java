package ai.humanitarians.pipeline.model.dto;

import ai.humanitarians.pipeline.model.enums.PipelineMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationReport {
    private OffsetDateTime runTimestamp = OffsetDateTime.now();
    private PipelineMode mode = PipelineMode.DRY_RUN;
    private int videosInspected = 0;
    private int newPublicationsDetected = 0;
    private int notificationsSent = 0;
    private int duplicatesPrevented = 0;
    private int notificationsSkipped = 0;
    private int failures = 0;
    private List<VideoProcessingResult> processedItems = new ArrayList<>();

    public ReconciliationReport() {}

    public ReconciliationReport(PipelineMode mode) {
        this.mode = mode;
    }

    public OffsetDateTime getRunTimestamp() { return runTimestamp; }
    public void setRunTimestamp(OffsetDateTime runTimestamp) { this.runTimestamp = runTimestamp; }

    public PipelineMode getMode() { return mode; }
    public void setMode(PipelineMode mode) { this.mode = mode; }

    public int getVideosInspected() { return videosInspected; }
    public void setVideosInspected(int videosInspected) { this.videosInspected = videosInspected; }

    public int getNewPublicationsDetected() { return newPublicationsDetected; }
    public void setNewPublicationsDetected(int newPublicationsDetected) { this.newPublicationsDetected = newPublicationsDetected; }

    public int getNotificationsSent() { return notificationsSent; }
    public void setNotificationsSent(int notificationsSent) { this.notificationsSent = notificationsSent; }

    public int getDuplicatesPrevented() { return duplicatesPrevented; }
    public void setDuplicatesPrevented(int duplicatesPrevented) { this.duplicatesPrevented = duplicatesPrevented; }

    public int getNotificationsSkipped() { return notificationsSkipped; }
    public void setNotificationsSkipped(int notificationsSkipped) { this.notificationsSkipped = notificationsSkipped; }

    public int getFailures() { return failures; }
    public void setFailures(int failures) { this.failures = failures; }

    public List<VideoProcessingResult> getProcessedItems() { return processedItems; }
    public void setProcessedItems(List<VideoProcessingResult> processedItems) { this.processedItems = processedItems; }

    public void incrementVideosInspected() { this.videosInspected++; }
    public void incrementNewPublications() { this.newPublicationsDetected++; }
    public void incrementNotificationsSent() { this.notificationsSent++; }
    public void incrementDuplicatesPrevented() { this.duplicatesPrevented++; }
    public void incrementNotificationsSkipped() { this.notificationsSkipped++; }
    public void incrementFailures() { this.failures++; }
}
