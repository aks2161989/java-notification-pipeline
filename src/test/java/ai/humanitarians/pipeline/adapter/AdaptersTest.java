package ai.humanitarians.pipeline.adapter;

import ai.humanitarians.pipeline.adapter.drive.MockDriveAdapter;
import ai.humanitarians.pipeline.adapter.email.EmailSink;
import ai.humanitarians.pipeline.adapter.email.FileOutboxEmailSink;
import ai.humanitarians.pipeline.adapter.youtube.MockYouTubeAdapter;
import ai.humanitarians.pipeline.adapter.youtube.YouTubeAdapter;
import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.DispatchResult;
import ai.humanitarians.pipeline.model.dto.DriveFileMetadata;
import ai.humanitarians.pipeline.model.dto.EmailNotification;
import ai.humanitarians.pipeline.model.dto.YouTubeVideoMetadata;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import ai.humanitarians.pipeline.service.EmailFormatter;
import ai.humanitarians.pipeline.service.NotificationDispatcherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdaptersTest {

    @Test
    @DisplayName("MockYouTubeAdapter reports lifecycle and handles simulated reviewer publishing")
    void testMockYouTubeAdapterLifecycle(@TempDir Path tempDir) {
        Path stateFile = tempDir.resolve(".mock_yt.json");
        MockYouTubeAdapter yt = new MockYouTubeAdapter(stateFile);

        // RzW17zhV4Nw starts unlisted in Queue
        assertFalse(yt.isVideoPublic("RzW17zhV4Nw"));
        Optional<YouTubeVideoMetadata> meta = yt.getVideoStatus("RzW17zhV4Nw");
        assertTrue(meta.isPresent());
        assertEquals("unlisted", meta.get().privacyStatus());
        assertEquals("Queue", meta.get().playlistName());

        // Reviewer hits publish
        assertTrue(yt.simulatePublish("RzW17zhV4Nw"));
        assertTrue(yt.isVideoPublic("RzW17zhV4Nw"));
        Optional<YouTubeVideoMetadata> metaAfter = yt.getVideoStatus("RzW17zhV4Nw");
        assertTrue(metaAfter.isPresent());
        assertEquals("public", metaAfter.get().privacyStatus());
        assertNotNull(metaAfter.get().publishedAt());

        // Simulate unpublish
        assertTrue(yt.simulateUnpublish("RzW17zhV4Nw"));
        assertFalse(yt.isVideoPublic("RzW17zhV4Nw"));
    }

    @Test
    @DisplayName("MockDriveAdapter retrieves file metadata")
    void testMockDriveAdapterMetadata() {
        MockDriveAdapter drive = new MockDriveAdapter();
        Optional<DriveFileMetadata> meta = drive.getFileMetadata("drive_file_101");
        assertTrue(meta.isPresent());
        assertEquals("Madison_Architecture_Final.mp4", meta.get().name());
        assertEquals("shah.jainil@northeastern.edu", meta.get().submitterEmail());
        assertEquals("video/mp4", meta.get().mimeType());
    }

    @Test
    @DisplayName("EmailFormatter renders subject prefixes, HTML layout, and delivery headers")
    void testEmailFormatterTemplates() {
        PipelineProperties props = new PipelineProperties();
        EmailFormatter formatter = new EmailFormatter(props);

        EmailNotification notif = formatter.render(
                1, 1, "Jainil Shah", "shah.jainil@northeastern.edu",
                "Madison Architecture", "RzW17zhV4Nw", "Madison",
                PipelineMode.DRY_RUN, null
        );

        assertTrue(notif.subject().contains("[DRY-RUN]"));
        assertTrue(notif.youtubeUrl().contains("https://www.youtube.com/watch?v=RzW17zhV4Nw"));
        assertTrue(notif.bodyText().contains("Madison Architecture"));
        assertTrue(notif.bodyHtml().contains("<h1>Humanitarians AI</h1>"));
        assertEquals("shah.jainil@northeastern.edu", notif.recipientEmail());
        assertEquals("shah.jainil@northeastern.edu", notif.headers().get("X-Original-Recipient"));
    }

    @Test
    @DisplayName("NotificationDispatcherService generates preview files in outbox for DRY_RUN mode")
    void testDispatcherDryRunMode(@TempDir Path tempDir) throws IOException {
        PipelineProperties props = new PipelineProperties();
        props.setOutboxDir(tempDir.toString());
        props.setMode(PipelineMode.DRY_RUN);

        EmailFormatter formatter = new EmailFormatter(props);
        FileOutboxEmailSink sink = new FileOutboxEmailSink(tempDir);
        NotificationDispatcherService dispatcher = new NotificationDispatcherService(props, formatter, List.of(sink));

        DispatchResult result = dispatcher.dispatch(
                10, 1, "Jainil Shah", "shah.jainil@northeastern.edu", "active",
                "Test Video", "yt123", "Madison", PipelineMode.DRY_RUN, null
        );

        assertTrue(result.success());
        assertEquals(NotificationDeliveryStatus.SKIPPED, result.deliveryStatus());
        assertTrue(result.reason().contains("Dry-run mode evaluated"));

        // Verify outbox files created
        List<Path> jsonFiles = Files.list(tempDir).filter(p -> p.toString().endsWith(".json")).toList();
        List<Path> htmlFiles = Files.list(tempDir).filter(p -> p.toString().endsWith(".html")).toList();
        assertEquals(1, jsonFiles.size());
        assertEquals(1, htmlFiles.size());

        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> payload = mapper.readValue(jsonFiles.get(0).toFile(), Map.class);
        assertEquals("shah.jainil@northeastern.edu", payload.get("intended_recipient"));
        assertEquals("DRY_RUN", payload.get("mode"));
    }

    @Test
    @DisplayName("NotificationDispatcherService redirects recipient address in REDIRECTED mode")
    void testDispatcherRedirectedMode(@TempDir Path tempDir) throws IOException {
        PipelineProperties props = new PipelineProperties();
        props.setOutboxDir(tempDir.toString());
        props.setMode(PipelineMode.REDIRECTED);

        EmailFormatter formatter = new EmailFormatter(props);
        FileOutboxEmailSink sink = new FileOutboxEmailSink(tempDir);
        NotificationDispatcherService dispatcher = new NotificationDispatcherService(props, formatter, List.of(sink));

        DispatchResult result = dispatcher.dispatch(
                20, 2, "Maya Lin", "m.lin@northeastern.edu", "active",
                "NeuroVEP Analysis", "yt456", "NeuroVEP",
                PipelineMode.REDIRECTED, "team.lead@example.com"
        );

        assertTrue(result.success());
        assertEquals(NotificationDeliveryStatus.SENT, result.deliveryStatus());
        assertEquals("team.lead@example.com", result.actualRecipient());

        List<Path> jsonFiles = Files.list(tempDir).filter(p -> p.toString().endsWith(".json")).toList();
        assertEquals(1, jsonFiles.size());
        ObjectMapper mapper = new ObjectMapper();
        Map<?, ?> payload = mapper.readValue(jsonFiles.get(0).toFile(), Map.class);
        assertEquals("team.lead@example.com", payload.get("recipient_email"));
        assertEquals("m.lin@northeastern.edu", payload.get("intended_recipient"));
    }

    @Test
    @DisplayName("NotificationDispatcherService skips inactive fellow with reason logged")
    void testDispatcherInactiveFellowSkipped() {
        PipelineProperties props = new PipelineProperties();
        EmailFormatter formatter = new EmailFormatter(props);
        NotificationDispatcherService dispatcher = new NotificationDispatcherService(props, formatter, List.of());

        DispatchResult result = dispatcher.dispatch(
                30, 3, "Inactive Fellow", "inactive@northeastern.edu", "inactive",
                "Old Video", "yt789", "Medhavi", PipelineMode.PROD, null
        );

        assertFalse(result.success());
        assertEquals(NotificationDeliveryStatus.SKIPPED, result.deliveryStatus());
        assertTrue(result.reason().contains("inactive"));
    }
}
