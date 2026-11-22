package ai.humanitarians.pipeline.config;

import ai.humanitarians.pipeline.model.enums.PipelineMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "pipeline")
public class PipelineProperties {

    private PipelineMode mode = PipelineMode.DRY_RUN;
    private String redirectTestEmail = "test@example.com";
    private List<Integer> cohortFellowIds = new ArrayList<>(List.of(1));
    private boolean useMockApis = true;
    private String youtubeApiKey = "";
    private String outboxDir = "outbox";
    private String trackerXlsxPath = "data/YouTube_Pipeline_Tracker.xlsx";
    private String lockFilePath = ".pipeline.lock";
    private int lockTimeoutSeconds = 600;
    private int maxRetryAttempts = 3;

    public PipelineMode getMode() { return mode; }
    public void setMode(PipelineMode mode) { this.mode = mode; }

    public String getRedirectTestEmail() { return redirectTestEmail; }
    public void setRedirectTestEmail(String redirectTestEmail) { this.redirectTestEmail = redirectTestEmail; }

    public List<Integer> getCohortFellowIds() { return cohortFellowIds; }
    public void setCohortFellowIds(String cohortFellowIds) {
        if (cohortFellowIds != null && !cohortFellowIds.isBlank()) {
            this.cohortFellowIds = Arrays.stream(cohortFellowIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        }
    }

    public boolean isUseMockApis() { return useMockApis; }
    public void setUseMockApis(boolean useMockApis) { this.useMockApis = useMockApis; }

    public String getYoutubeApiKey() { return youtubeApiKey; }
    public void setYoutubeApiKey(String youtubeApiKey) { this.youtubeApiKey = youtubeApiKey; }

    public String getOutboxDir() { return outboxDir; }
    public void setOutboxDir(String outboxDir) { this.outboxDir = outboxDir; }

    public String getTrackerXlsxPath() { return trackerXlsxPath; }
    public void setTrackerXlsxPath(String trackerXlsxPath) { this.trackerXlsxPath = trackerXlsxPath; }

    public String getLockFilePath() { return lockFilePath; }
    public void setLockFilePath(String lockFilePath) { this.lockFilePath = lockFilePath; }

    public int getLockTimeoutSeconds() { return lockTimeoutSeconds; }
    public void setLockTimeoutSeconds(int lockTimeoutSeconds) { this.lockTimeoutSeconds = lockTimeoutSeconds; }

    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
}
