package ai.humanitarians.pipeline.adapter.youtube;

import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public class LiveYouTubeAdapter implements YouTubeAdapter {

    private static final Logger log = LoggerFactory.getLogger(LiveYouTubeAdapter.class);
    private final String apiKey;
    private final YouTube youTubeClient;

    public LiveYouTubeAdapter(String apiKey) {
        this.apiKey = apiKey;
        if (apiKey != null && !apiKey.isBlank()) {
            this.youTubeClient = new YouTube.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    request -> {}
            ).setApplicationName("HumanitariansAI-NotificationPipeline").build();
        } else {
            this.youTubeClient = null;
        }
    }

    @Override
    public Optional<YouTubeVideoMetadata> getVideoStatus(String videoId) {
        if (youTubeClient == null || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YOUTUBE_API_KEY not configured. Set USE_MOCK_APIS=true or provide a valid key.");
        }
        try {
            YouTube.Videos.List request = youTubeClient.videos().list(List.of("status", "snippet"));
            request.setId(List.of(videoId));
            request.setKey(apiKey);

            VideoListResponse response = request.execute();
            List<Video> items = response.getItems();
            if (items == null || items.isEmpty()) {
                return Optional.empty();
            }

            Video video = items.get(0);
            String title = video.getSnippet() != null ? video.getSnippet().getTitle() : "";
            String privacy = video.getStatus() != null ? video.getStatus().getPrivacyStatus() : "unknown";
            String uploadStatus = video.getStatus() != null ? video.getStatus().getUploadStatus() : "unknown";
            String channelId = video.getSnippet() != null ? video.getSnippet().getChannelId() : null;

            OffsetDateTime publishedAt = null;
            if (video.getSnippet() != null && video.getSnippet().getPublishedAt() != null) {
                publishedAt = OffsetDateTime.parse(video.getSnippet().getPublishedAt().toStringRfc3339());
            }

            return Optional.of(new YouTubeVideoMetadata(
                    videoId, title, privacy, uploadStatus, publishedAt, channelId, null
            ));
        } catch (Exception e) {
            log.error("Failed to fetch YouTube status for video {}: {}", videoId, e.getMessage());
            throw new RuntimeException("YouTube API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isVideoPublic(String videoId) {
        return getVideoStatus(videoId).map(YouTubeVideoMetadata::isPublic).orElse(false);
    }
}
