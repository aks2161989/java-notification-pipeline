package ai.humanitarians.pipeline.adapter.youtube;

import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
            // Fallback to public oEmbed check
            if (checkYouTubeOEmbed(videoId)) {
                return Optional.of(new YouTubeVideoMetadata(
                        videoId, "Public Video " + videoId, "public", "processed",
                        OffsetDateTime.now(), null, null
                ));
            }
            return Optional.empty();
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

    private boolean checkYouTubeOEmbed(String videoId) {
        if (videoId == null || videoId.isBlank()) return false;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            String url = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + videoId + "&format=json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.debug("oEmbed check for video {} returned: {}", videoId, e.getMessage());
            return false;
        }
    }
}
