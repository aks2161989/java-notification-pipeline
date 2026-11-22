package ai.humanitarians.pipeline.scheduler;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PipelineCronSchedulerTest {

    private ReconciliationService reconciliationService;
    private PipelineProperties properties;
    private PipelineCronScheduler scheduler;

    @BeforeEach
    void setUp() {
        reconciliationService = Mockito.mock(ReconciliationService.class);
        properties = new PipelineProperties();
        scheduler = new PipelineCronScheduler(reconciliationService, properties);
    }

    @Test
    @DisplayName("Scheduled execution is skipped when cron is disabled")
    void testExecutionSkippedWhenDisabled() {
        properties.getCron().setEnabled(false);

        scheduler.executeScheduledReconciliation();

        verify(reconciliationService, never()).runReconciliation();
        assertEquals(0, scheduler.getExecutionCount());
        assertNull(scheduler.getLastRunTime());
    }

    @Test
    @DisplayName("Scheduled execution runs reconciliation when cron is enabled")
    void testExecutionRunsWhenEnabled() {
        properties.getCron().setEnabled(true);
        ReconciliationReport mockReport = new ReconciliationReport(PipelineMode.DRY_RUN);
        mockReport.setVideosInspected(3);
        when(reconciliationService.runReconciliation()).thenReturn(mockReport);

        scheduler.executeScheduledReconciliation();

        verify(reconciliationService, times(1)).runReconciliation();
        assertEquals(1, scheduler.getExecutionCount());
        assertNotNull(scheduler.getLastRunTime());
        assertNotNull(scheduler.getLastCompletionTime());
        assertNotNull(scheduler.getLastReport());
        assertEquals(3, scheduler.getLastReport().getVideosInspected());
        assertNull(scheduler.getLastError());
    }

    @Test
    @DisplayName("On-demand triggerNow executes reconciliation immediately regardless of enabled flag")
    void testTriggerNowExecutesImmediately() {
        properties.getCron().setEnabled(false);
        ReconciliationReport mockReport = new ReconciliationReport(PipelineMode.DRY_RUN);
        when(reconciliationService.runReconciliation()).thenReturn(mockReport);

        ReconciliationReport result = scheduler.triggerNow();

        verify(reconciliationService, times(1)).runReconciliation();
        assertNotNull(result);
        assertEquals(1, scheduler.getExecutionCount());
    }

    @Test
    @DisplayName("getStatus returns comprehensive scheduler metrics")
    void testGetStatus() {
        properties.getCron().setEnabled(true);
        properties.getCron().setSchedule("0 */10 * * * *");

        Map<String, Object> status = scheduler.getStatus();
        assertTrue((Boolean) status.get("enabled"));
        assertEquals("0 */10 * * * *", status.get("schedule"));
        assertEquals(0L, status.get("executionCount"));
    }
}
