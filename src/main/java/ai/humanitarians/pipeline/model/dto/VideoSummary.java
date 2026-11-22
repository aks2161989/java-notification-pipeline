package ai.humanitarians.pipeline.model.dto;

import ai.humanitarians.pipeline.model.entity.VideoRecord;

import java.time.OffsetDateTime;

public record VideoSummary(
        Integer videoId,
        Integer fellowId,
        String fellowName,
        String fellowEmail,
        String projectName,
        String title,
        String youtubeVideoId,
        String youtubeLink,
        String driveLink,
        String currentStatus,
        String statusNote,
        OffsetDateTime submittedOn,
        OffsetDateTime publishedOn
) {
    public static VideoSummary from(VideoRecord v) {
        Integer fid = null;
        String fName = null;
        String fEmail = null;
        String pName = null;
        if (v.getFellow() != null) {
            fid = v.getFellow().getFellowId();
            fName = v.getFellow().getFullName();
            fEmail = v.getFellow().getEmail();
            if (v.getFellow().getProject() != null) {
                pName = v.getFellow().getProject().getProjectName();
            }
        }
        return new VideoSummary(
                v.getVideoId(),
                fid,
                fName,
                fEmail,
                pName,
                v.getTitle(),
                v.getYoutubeVideoId(),
                v.getYoutubeLink(),
                v.getDriveLink(),
                v.getCurrentStatus() != null ? v.getCurrentStatus().getValue() : null,
                v.getStatusNote(),
                v.getSubmittedOn(),
                v.getPublishedOn()
        );
    }
}
