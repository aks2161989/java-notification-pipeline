package ai.humanitarians.pipeline.model.dto;

import ai.humanitarians.pipeline.model.entity.Fellow;

public record FellowSummary(
        Integer fellowId,
        String fullName,
        String email,
        String status,
        String displayLabel,
        Integer projectId,
        String projectName
) {
    public static FellowSummary from(Fellow f) {
        Integer pid = f.getProject() != null ? f.getProject().getProjectId() : null;
        String pName = f.getProject() != null ? f.getProject().getProjectName() : "General / Unassigned";
        return new FellowSummary(
                f.getFellowId(),
                f.getFullName(),
                f.getEmail(),
                f.getStatus(),
                f.getDisplayLabel(),
                pid,
                pName
        );
    }
}
