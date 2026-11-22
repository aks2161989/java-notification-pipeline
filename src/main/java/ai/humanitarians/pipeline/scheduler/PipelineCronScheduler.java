package ai.humanitarians.pipeline.scheduler;

import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "pipeline.cron.enabled", havingValue = "true")
public class PipelineCronScheduler {

    private static final Logger log = LoggerFactory.getLogger(PipelineCronScheduler.class);

    private final ReconciliationService reconciliationService;

    public PipelineCronScheduler(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "${pipeline.cron.schedule:0 */15 * * * *}")
    public void executeScheduledReconciliation() {
        log.info("Starting scheduled reconciliation run...");
        try {
            ReconciliationReport report = reconciliationService.runReconciliation();
            log.info("Scheduled reconciliation completed: Inspected={}, Detected={}, Sent={}, DuplicatesPrevented={}, Failures={}",
                    report.getVideosInspected(), report.getNewPublicationsDetected(),
                    report.getNotificationsSent(), report.getDuplicatesPrevented(), report.getFailures());
        } catch (Exception e) {
            log.error("Scheduled reconciliation failed: {}", e.getMessage(), e);
        }
    }
}
