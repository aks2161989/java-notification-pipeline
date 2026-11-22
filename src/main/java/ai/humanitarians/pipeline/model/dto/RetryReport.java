package ai.humanitarians.pipeline.model.dto;

import java.util.ArrayList;
import java.util.List;

public class RetryReport {
    private int retriedCount = 0;
    private int succeededCount = 0;
    private int exhaustedCount = 0;
    private List<String> details = new ArrayList<>();

    public int getRetriedCount() { return retriedCount; }
    public void setRetriedCount(int retriedCount) { this.retriedCount = retriedCount; }

    public int getSucceededCount() { return succeededCount; }
    public void setSucceededCount(int succeededCount) { this.succeededCount = succeededCount; }

    public int getExhaustedCount() { return exhaustedCount; }
    public void setExhaustedCount(int exhaustedCount) { this.exhaustedCount = exhaustedCount; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
