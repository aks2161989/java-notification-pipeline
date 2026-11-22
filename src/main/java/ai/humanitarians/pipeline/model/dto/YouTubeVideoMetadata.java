package ai.humanitarians.pipeline.model.dto;

import java.time.OffsetDateTime;

public record YouTubeVideoMetadata(
        String videoId,
        String title,
        String privacyStatus,
        String uploadStatus,
        OffsetDateTime publishedAt,
        String channelId,
        String playlistName
) {
    public boolean isPublic() {
        return privacyStatus != null && "public".equalsIgnoreCase(privacyStatus);
    }
}
