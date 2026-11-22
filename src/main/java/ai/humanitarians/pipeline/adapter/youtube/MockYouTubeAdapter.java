package ai.humanitarians.pipeline.adapter.youtube;

import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (existing != null) {
            videos.put(videoId, new YouTubeVideoMetadata(
                    existing.videoId(),
                    existing.title(),
                    "public",
                    existing.uploadStatus(),
                    OffsetDateTime.now(),
                    existing.channelId(),
                    existing.playlistName()
            ));
            persist();
            return true;
        }
        return false;
    }

    public boolean simulateUnpublish(String videoId) {
        YouTubeVideoMetadata existing = videos.get(videoId);
        if (existing != null) {
            videos.put(videoId, new YouTubeVideoMetadata(
                    existing.videoId(),
                    existing.title(),
                    "unlisted",
                    existing.uploadStatus(),
                    null,
                    existing.channelId(),
                    existing.playlistName()
            ));
            persist();
            return true;
        }
        return false;
    }

    @Override
    public Optional<YouTubeVideoMetadata> getVideoStatus(String videoId) {
        return Optional.ofNullable(videos.get(videoId));
    }

    @Override
    public boolean isVideoPublic(String videoId) {
        return getVideoStatus(videoId).map(YouTubeVideoMetadata::isPublic).orElse(false);
    }
}
