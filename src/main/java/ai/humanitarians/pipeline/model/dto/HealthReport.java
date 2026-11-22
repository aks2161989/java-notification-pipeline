package ai.humanitarians.pipeline.model.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthReport {
    public record HealthIssue(
            String severity,     // CRITICAL, WARNING, INFO
            String category,     // DATA_QUALITY, STRANDED_JOB, RETRY_EXHAUSTED, LOCK
            String description,
            Integer recordId
    ) {}

    private boolean healthy = true;
    private OffsetDateTime inspectedAt = OffsetDateTime.now();
    private List<HealthIssue> issues = new ArrayList<>();
    private Map<String, Object> stats = new HashMap<>();

    public HealthReport() {}

    public HealthReport(boolean healthy, List<HealthIssue> issues, Map<String, Object> stats) {
        this.healthy = healthy;
        this.issues = issues != null ? issues : new ArrayList<>();
        this.stats = stats != null ? stats : new HashMap<>();
    }

    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }

    public OffsetDateTime getInspectedAt() { return inspectedAt; }
    public void setInspectedAt(OffsetDateTime inspectedAt) { this.inspectedAt = inspectedAt; }

    public List<HealthIssue> getIssues() { return issues; }
    public void setIssues(List<HealthIssue> issues) { this.issues = issues; }

    public Map<String, Object> getStats() { return stats; }
    public void setStats(Map<String, Object> stats) { this.stats = stats; }
}
