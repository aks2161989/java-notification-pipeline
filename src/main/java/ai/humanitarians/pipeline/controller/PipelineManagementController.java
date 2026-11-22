package ai.humanitarians.pipeline.controller;

import ai.humanitarians.pipeline.adapter.youtube.MockYouTubeAdapter;
import ai.humanitarians.pipeline.adapter.youtube.YouTubeAdapter;
import ai.humanitarians.pipeline.model.dto.*;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/pipeline")
public class PipelineManagementController {

    private final ReconciliationService reconciliationService;
    private final PipelineHealthService healthService;
    private final NotificationRetryService retryService;
    private final DatabaseSeederService seederService;
    private final YouTubeAdapter youTubeAdapter;

    public PipelineManagementController(
            ReconciliationService reconciliationService,
            PipelineHealthService healthService,
            NotificationRetryService retryService,
            DatabaseSeederService seederService,
            YouTubeAdapter youTubeAdapter
    ) {
        this.reconciliationService = reconciliationService;
        this.healthService = healthService;
        this.retryService = retryService;
        this.seederService = seederService;
        this.youTubeAdapter = youTubeAdapter;
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationReport> reconcile(
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String redirectTo,
            @RequestParam(defaultValue = "false") boolean bypassLock
    ) {
        PipelineMode effMode = mode != null ? PipelineMode.fromString(mode) : null;
        ReconciliationReport report = reconciliationService.runReconciliation(effMode, redirectTo, bypassLock);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/status")
    public ResponseEntity<MetricsSummary> getStatus() {
        return ResponseEntity.ok(healthService.getMetrics());
    }

    @GetMapping("/health")
    public ResponseEntity<HealthReport> getHealth() {
        return ResponseEntity.ok(healthService.checkHealth());
    }

    @PostMapping("/retry")
    public ResponseEntity<RetryReport> retryFailed(
            @RequestParam(defaultValue = "3") int maxAttempts,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String redirectTo
    ) {
        PipelineMode effMode = mode != null ? PipelineMode.fromString(mode) : null;
        RetryReport report = retryService.retryFailed(maxAttempts, effMode, redirectTo);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/seed")
    public ResponseEntity<Map<String, Integer>> seedDatabase(@RequestParam(defaultValue = "false") boolean syntheticOnly) {
        if (syntheticOnly) {
            return ResponseEntity.ok(seederService.seedSyntheticScenarios());
        }
        return ResponseEntity.ok(seederService.seedFromExcel(Path.of("data/YouTube_Pipeline_Tracker.xlsx")));
    }

    @PostMapping("/simulation/publish/{videoId}")
    public ResponseEntity<Map<String, Object>> simulatePublish(@PathVariable String videoId) {
        if (youTubeAdapter instanceof MockYouTubeAdapter mock) {
            boolean success = mock.simulatePublish(videoId);
            return ResponseEntity.ok(Map.of("videoId", videoId, "published", success));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Mock adapter not active"));
    }

    @PostMapping("/simulation/unpublish/{videoId}")
    public ResponseEntity<Map<String, Object>> simulateUnpublish(@PathVariable String videoId) {
        if (youTubeAdapter instanceof MockYouTubeAdapter mock) {
            boolean success = mock.simulateUnpublish(videoId);
            return ResponseEntity.ok(Map.of("videoId", videoId, "unpublished", success));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Mock adapter not active"));
    }
}
