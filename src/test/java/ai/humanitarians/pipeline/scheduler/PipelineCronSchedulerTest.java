package ai.humanitarians.pipeline.scheduler;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.ReconciliationReport;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.service.DatabaseSeederService;
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
    private DatabaseSeederService seederService;
    private PipelineProperties properties;
    private PipelineCronScheduler scheduler;

    @BeforeEach
    void setUp() {
        reconciliationService = Mockito.mock(ReconciliationService.class);
        seederService = Mockito.mock(DatabaseSeederService.class);
        properties = new PipelineProperties();
        scheduler = new PipelineCronScheduler(reconciliationService, seederService, properties);
    }

    @Test
    @DisplayName("Scheduled execution is skipped when cron is disabled")
    void testExecutionSkippedWhenDisabled() {
        properties.getCron().setEnabled(false);

        scheduler.executeScheduledReconciliation();

        verify(reconciliationService, never()).runReconciliation();
        verify(seederService, never()).syncFromGoogleSheetWorkbook();
        assertEquals(0, scheduler.getExecutionCount());
        assertNull(scheduler.getLastRunTime());
    }

    @Test
    @DisplayName("Scheduled execution auto-syncs Google Sheet and runs reconciliation when cron is enabled")
    void testExecutionRunsWhenEnabled() {
        properties.getCron().setEnabled(true);
        properties.getCron().setAutoSyncSheet(true);
        ReconciliationReport mockReport = new ReconciliationReport(PipelineMode.DRY_RUN);
        mockReport.setVideosInspected(3);
        when(reconciliationService.runReconciliation()).thenReturn(mockReport);

        scheduler.executeScheduledReconciliation();

        verify(seederService, times(1)).syncFromGoogleSheetWorkbook();
        verify(reconciliationService, times(1)).runReconciliation();
        assertEquals(1, scheduler.getExecutionCount());
        assertNotNull(scheduler.getLastRunTime());
        assertNotNull(scheduler.getLastSheetSyncTime());
        assertNotNull(scheduler.getLastCompletionTime());
        assertNotNull(scheduler.getLastReport());
        assertEquals(3, scheduler.getLastReport().getVideosInspected());
        assertNull(scheduler.getLastError());
    }

    @Test
    @DisplayName("On-demand triggerNow executes reconciliation immediately regardless of enabled flag")
    void testTriggerNowExecutesImmediately() {
        properties.getCron().setEnabled(false);
        properties.getCron().setAutoSyncSheet(false);
        ReconciliationReport mockReport = new ReconciliationReport(PipelineMode.DRY_RUN);
        when(reconciliationService.runReconciliation()).thenReturn(mockReport);

        ReconciliationReport result = scheduler.triggerNow();

        verify(seederService, never()).syncFromGoogleSheetWorkbook();
        verify(reconciliationService, times(1)).runReconciliation();
        assertNotNull(result);
        assertEquals(1, scheduler.getExecutionCount());
    }

    @Test
    @DisplayName("getStatus returns comprehensive scheduler metrics including autoSyncSheet")
    void testGetStatus() {
        properties.getCron().setEnabled(true);
        properties.getCron().setAutoSyncSheet(true);
        properties.getCron().setSchedule("0 */10 * * * *");

        Map<String, Object> status = scheduler.getStatus();
        assertTrue((Boolean) status.get("enabled"));
        assertTrue((Boolean) status.get("autoSyncSheet"));
        assertEquals("0 */10 * * * *", status.get("schedule"));
        assertEquals(0L, status.get("executionCount"));
    }
}
