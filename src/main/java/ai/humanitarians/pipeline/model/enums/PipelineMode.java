package ai.humanitarians.pipeline.model.enums;

public enum PipelineMode {
    DRY_RUN,
    REDIRECTED,
    COHORT,
    PROD;

    public static PipelineMode fromString(String val) {
        if (val == null || val.isBlank()) return DRY_RUN;
        return PipelineMode.valueOf(val.trim().toUpperCase());
    }
}
