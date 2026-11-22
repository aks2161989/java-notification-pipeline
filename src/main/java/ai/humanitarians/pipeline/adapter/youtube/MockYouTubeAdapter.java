package ai.humanitarians.pipeline.adapter.youtube;

import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockYouTubeAdapter implements YouTubeAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockYouTubeAdapter.class);
    private final Path stateFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, YouTubeVideoMetadata> videos = new ConcurrentHashMap<>();

    public MockYouTubeAdapter() {
        this(Path.of(".mock_youtube.json"));
    }

    public MockYouTubeAdapter(Path stateFile) {
        this.stateFile = stateFile;
        initDefaults();
        loadSavedState();
    }

    private void initDefaults() {
        registerVideo(new YouTubeVideoMetadata(
                "RzW17zhV4Nw", "Madison System Architecture Overview", "unlisted", "processed",
                null, null, "Queue"
        ));
        registerVideo(new YouTubeVideoMetadata(
                "dQw4w9WgXcQ", "NeuroVEP Visual Evoked Potential Analysis", "unlisted", "processed",
                null, null, "Queue"
        ));
        registerVideo(new YouTubeVideoMetadata(
                "3C_zAB12345", "Lyrical Literacy Interactive Learning Demo", "public", "processed",
                null, null, null
        ));
        registerVideo(new YouTubeVideoMetadata(
                "4D_wCD67890", "Medhavi Assistive Tech Walkthrough", "unlisted", "processed",
                null, null, "Queue"
        ));
        registerVideo(new YouTubeVideoMetadata(
                "5E_vEF99999", "Caching Explained in 5 Minutes", "private", "processed",
                null, null, null
        ));
    }

    private void loadSavedState() {
        if (stateFile != null && Files.exists(stateFile)) {
            try {
                Map<String, Map<String, Object>> saved = objectMapper.readValue(
                        stateFile.toFile(),
                        new TypeReference<>() {}
                );
                for (Map.Entry<String, Map<String, Object>> entry : saved.entrySet()) {
                    String vid = entry.getKey();
                    Map<String, Object> data = entry.getValue();
                    videos.put(vid, new YouTubeVideoMetadata(
                            vid,
                            (String) data.getOrDefault("title", ""),
                            (String) data.getOrDefault("privacy_status", "unlisted"),
                            (String) data.getOrDefault("upload_status", "processed"),
                            null,
                            null,
                            (String) data.get("playlist_name")
                    ));
                }
            } catch (Exception e) {
                log.warn("Could not read mock YouTube state file: {}", e.getMessage());
            }
        }
    }

    private void persist() {
        if (stateFile == null) return;
        try {
            Map<String, Object> raw = new HashMap<>();
            for (Map.Entry<String, YouTubeVideoMetadata> entry : videos.entrySet()) {
                YouTubeVideoMetadata m = entry.getValue();
                Map<String, Object> item = new HashMap<>();
                item.put("title", m.title());
                item.put("privacy_status", m.privacyStatus());
                item.put("upload_status", m.uploadStatus());
                item.put("playlist_name", m.playlistName());
                raw.put(entry.getKey(), item);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(stateFile.toFile(), raw);
        } catch (Exception e) {
            log.warn("Could not persist mock YouTube state: {}", e.getMessage());
        }
    }

    public void registerVideo(YouTubeVideoMetadata meta) {
        videos.put(meta.videoId(), meta);
        persist();
    }

    public boolean simulatePublish(String videoId) {
        YouTubeVideoMetadata existing = videos.get(videoId);
        String title = existing != null ? existing.title() : "Video " + videoId;
        String channelId = existing != null ? existing.channelId() : null;
        videos.put(videoId, new YouTubeVideoMetadata(
                videoId,
                title,
                "public",
                "processed",
                OffsetDateTime.now(),
                channelId,
                null
        ));
        persist();
        return true;
    }

    public boolean simulateUnpublish(String videoId) {
        YouTubeVideoMetadata existing = videos.get(videoId);
        String title = existing != null ? existing.title() : "Video " + videoId;
        String channelId = existing != null ? existing.channelId() : null;
        videos.put(videoId, new YouTubeVideoMetadata(
                videoId,
                title,
                "unlisted",
                "processed",
                null,
                channelId,
                "Queue"
        ));
        persist();
        return true;
    }

    @Override
    public Optional<YouTubeVideoMetadata> getVideoStatus(String videoId) {
        // 1. Check if this is a real YouTube video via oEmbed
        Optional<String> realTitle = checkYouTubeOEmbedTitle(videoId);
        if (realTitle.isPresent()) {
            boolean isUnlisted = isLiveVideoUnlisted(videoId);
            String privacy = isUnlisted ? "unlisted" : "public";
            YouTubeVideoMetadata meta = videos.get(videoId);
            YouTubeVideoMetadata liveMeta = new YouTubeVideoMetadata(
                    videoId,
                    meta != null && meta.title() != null && !meta.title().isBlank() ? meta.title() : realTitle.get(),
                    privacy,
                    "processed",
                    "public".equals(privacy) ? (meta != null && meta.publishedAt() != null ? meta.publishedAt() : OffsetDateTime.now()) : null,
                    meta != null ? meta.channelId() : null,
                    isUnlisted ? "Queue" : null
            );
            videos.put(videoId, liveMeta);
            persist();
            return Optional.of(liveMeta);
        }

        // 2. Otherwise fall back to synthetic mock map (for offline unit tests)
        YouTubeVideoMetadata meta = videos.get(videoId);
        if (meta != null) {
            return Optional.of(meta);
        }
        return Optional.empty();
    }

    private boolean isLiveVideoUnlisted(String videoId) {
        if (videoId == null || videoId.isBlank()) return false;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            String url = "https://www.youtube.com/watch?v=" + videoId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                return resp.body().contains("\"isUnlisted\":true") || resp.body().contains("\"isUnlisted\": true");
            }
            return false;
        } catch (Exception e) {
            log.debug("Live watch page check for video {} returned: {}", videoId, e.getMessage());
            return false;
        }
    }

    private Optional<String> checkYouTubeOEmbedTitle(String videoId) {
        if (videoId == null || videoId.isBlank()) return Optional.empty();
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
            if (resp.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(resp.body());
                if (root.has("title")) {
                    return Optional.of(root.get("title").asText());
                }
                return Optional.of("YouTube Video " + videoId);
            }
            return Optional.empty();
        } catch (Exception e) {
            log.debug("oEmbed check for video {} returned: {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean checkYouTubeOEmbed(String videoId) {
        return checkYouTubeOEmbedTitle(videoId).isPresent();
    }

    @Override
    public boolean isVideoPublic(String videoId) {
        return getVideoStatus(videoId).map(YouTubeVideoMetadata::isPublic).orElse(false);
    }
}
