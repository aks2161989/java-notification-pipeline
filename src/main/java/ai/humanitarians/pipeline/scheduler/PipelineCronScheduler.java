package ai.humanitarians.pipeline.scheduler;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class PipelineCronScheduler {

    private static final Logger log = LoggerFactory.getLogger(PipelineCronScheduler.class);

    private final ReconciliationService reconciliationService;
    private final PipelineProperties properties;

    private OffsetDateTime lastRunTime;
    private OffsetDateTime lastCompletionTime;
    private ReconciliationReport lastReport;
    private long executionCount = 0;
    private String lastError;

    public PipelineCronScheduler(ReconciliationService reconciliationService, PipelineProperties properties) {
        this.reconciliationService = reconciliationService;
        this.properties = properties;
    }

    @Scheduled(cron = "${pipeline.cron.schedule:0 */15 * * * *}")
    public void executeScheduledReconciliation() {
        if (!properties.getCron().isEnabled()) {
            return;
        }
        triggerReconciliation("scheduled");
    }

    public ReconciliationReport triggerNow() {
        return triggerReconciliation("manual");
    }

    private synchronized ReconciliationReport triggerReconciliation(String triggerType) {
        log.info("Starting {} reconciliation run #{}...", triggerType, ++executionCount);
        this.lastRunTime = OffsetDateTime.now();
        this.lastError = null;
        try {
            ReconciliationReport report = reconciliationService.runReconciliation();
            this.lastReport = report;
            this.lastCompletionTime = OffsetDateTime.now();
            log.info("{} reconciliation run #{} completed: Inspected={}, Detected={}, Sent={}, DuplicatesPrevented={}, Failures={}",
                    triggerType, executionCount,
                    report.getVideosInspected(), report.getNewPublicationsDetected(),
                    report.getNotificationsSent(), report.getDuplicatesPrevented(), report.getFailures());
            return report;
        } catch (Exception e) {
            this.lastError = e.getMessage();
            this.lastCompletionTime = OffsetDateTime.now();
            log.error("{} reconciliation run #{} failed: {}", triggerType, executionCount, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", properties.getCron().isEnabled());
        status.put("schedule", properties.getCron().getSchedule());
        status.put("executionCount", executionCount);
        status.put("lastRunTime", lastRunTime);
        status.put("lastCompletionTime", lastCompletionTime);
        status.put("lastError", lastError);
        status.put("lastReport", lastReport);
        return status;
    }

    public OffsetDateTime getLastRunTime() { return lastRunTime; }
    public OffsetDateTime getLastCompletionTime() { return lastCompletionTime; }
    public ReconciliationReport getLastReport() { return lastReport; }
    public long getExecutionCount() { return executionCount; }
    public String getLastError() { return lastError; }
}
