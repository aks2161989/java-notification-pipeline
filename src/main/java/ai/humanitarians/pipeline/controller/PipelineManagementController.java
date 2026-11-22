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
    private final ai.humanitarians.pipeline.repository.FellowRepository fellowRepository;
    private final ai.humanitarians.pipeline.repository.VideoRecordRepository videoRepository;
    private final ai.humanitarians.pipeline.config.PipelineProperties properties;

    public PipelineManagementController(
            ReconciliationService reconciliationService,
            PipelineHealthService healthService,
            NotificationRetryService retryService,
            DatabaseSeederService seederService,
            YouTubeAdapter youTubeAdapter,
            ai.humanitarians.pipeline.repository.FellowRepository fellowRepository,
            ai.humanitarians.pipeline.repository.VideoRecordRepository videoRepository,
            ai.humanitarians.pipeline.config.PipelineProperties properties
    ) {
        this.reconciliationService = reconciliationService;
        this.healthService = healthService;
        this.retryService = retryService;
        this.seederService = seederService;
        this.youTubeAdapter = youTubeAdapter;
        this.fellowRepository = fellowRepository;
        this.videoRepository = videoRepository;
        this.properties = properties;
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
    public ResponseEntity<Map<String, Integer>> seedDatabase(
            @RequestParam(defaultValue = "false") boolean syntheticOnly,
            @RequestParam(defaultValue = "false") boolean localOnly
    ) {
        if (syntheticOnly) {
            return ResponseEntity.ok(seederService.seedSyntheticScenarios());
        }
        if (localOnly) {
            return ResponseEntity.ok(seederService.seedFromExcel(Path.of(properties.getTrackerXlsxPath())));
        }
        try {
            return ResponseEntity.ok(seederService.syncFromGoogleSheetWorkbook());
        } catch (Exception e) {
            return ResponseEntity.ok(seederService.seedFromExcel(Path.of(properties.getTrackerXlsxPath())));
        }
    }

    @PostMapping("/seed/workbook")
    public ResponseEntity<Map<String, Integer>> syncWorkbook(
            @RequestParam(required = false) String workbookUrl
    ) {
        Map<String, Integer> result = (workbookUrl != null && !workbookUrl.isBlank())
                ? seederService.syncFromGoogleSheetWorkbook(workbookUrl)
                : seederService.syncFromGoogleSheetWorkbook();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/seed/fellows")
    public ResponseEntity<Map<String, Integer>> syncFellows(
            @RequestParam(required = false) String sheetUrl
    ) {
        Map<String, Integer> result = (sheetUrl != null && !sheetUrl.isBlank())
                ? seederService.syncFellowsFromGoogleSheet(sheetUrl)
                : seederService.syncFellowsFromGoogleSheet();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/fellows")
    public ResponseEntity<java.util.List<ai.humanitarians.pipeline.model.dto.FellowSummary>> getFellows(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit
    ) {
        java.util.List<ai.humanitarians.pipeline.model.entity.Fellow> list = (status != null && !status.isBlank())
                ? fellowRepository.findByStatusWithProject(status)
                : fellowRepository.findAllWithProject();
        if (limit > 0 && list.size() > limit) {
            list = list.subList(0, limit);
        }
        java.util.List<ai.humanitarians.pipeline.model.dto.FellowSummary> dtos = list.stream()
                .map(ai.humanitarians.pipeline.model.dto.FellowSummary::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/videos")
    public ResponseEntity<java.util.List<ai.humanitarians.pipeline.model.dto.VideoSummary>> getVideos(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit
    ) {
        java.util.List<ai.humanitarians.pipeline.model.entity.VideoRecord> list;
        if (status != null && !status.isBlank()) {
            ai.humanitarians.pipeline.model.enums.VideoStatus vs = ai.humanitarians.pipeline.model.enums.VideoStatus.fromValue(status);
            list = videoRepository.findByCurrentStatus(vs);
        } else {
            list = videoRepository.findAllWithFellow();
        }
        if (limit > 0 && list.size() > limit) {
            list = list.subList(0, limit);
        }
        java.util.List<ai.humanitarians.pipeline.model.dto.VideoSummary> dtos = list.stream()
                .map(ai.humanitarians.pipeline.model.dto.VideoSummary::from)
                .toList();
        return ResponseEntity.ok(dtos);
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
