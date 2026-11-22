package ai.humanitarians.pipeline.adapter.email;

import ai.humanitarians.pipeline.model.dto.EmailNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileOutboxEmailSink implements EmailSink {

    private static final Logger log = LoggerFactory.getLogger(FileOutboxEmailSink.class);
    private final Path outboxDir;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public FileOutboxEmailSink(Path outboxDir) {
        this.outboxDir = outboxDir;
        try {
            Files.createDirectories(outboxDir);
        } catch (IOException e) {
            log.warn("Failed to create outbox directory {}: {}", outboxDir, e.getMessage());
        }
    }

    @Override
    public boolean deliver(EmailNotification n) {
        String timestampStr = n.createdAt().format(FORMATTER);
        String baseName = String.format("video_%d_%s", n.videoId(), timestampStr);

        try {
            Files.createDirectories(outboxDir);

            // 1. JSON metadata file
            Path jsonPath = outboxDir.resolve(baseName + ".json");
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("video_id", n.videoId());
            meta.put("fellow_id", n.fellowId());
            meta.put("fellow_name", n.fellowName());
            meta.put("recipient_email", n.recipientEmail());
            meta.put("intended_recipient", n.intendedRecipient());
            meta.put("subject", n.subject());
            meta.put("youtube_url", n.youtubeUrl());
            meta.put("mode", n.mode().name());
            meta.put("created_at", n.createdAt().toString());
            meta.put("headers", n.headers());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonPath.toFile(), meta);

            // 2. HTML preview file
            Path htmlPath = outboxDir.resolve(baseName + ".html");
            Files.writeString(htmlPath, n.bodyHtml(), StandardCharsets.UTF_8);

            return true;
        } catch (IOException e) {
            log.error("Failed to write email outbox file: {}", e.getMessage(), e);
            return false;
        }
    }
}
