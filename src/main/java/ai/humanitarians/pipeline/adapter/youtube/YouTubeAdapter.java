package ai.humanitarians.pipeline.adapter.youtube;

import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import java.util.Optional;

public interface YouTubeAdapter {
    Optional<YouTubeVideoMetadata> getVideoStatus(String videoId);
    boolean isVideoPublic(String videoId);
}
