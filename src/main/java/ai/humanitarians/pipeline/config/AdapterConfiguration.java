package ai.humanitarians.pipeline.config;

import ai.humanitarians.pipeline.adapter.drive.DriveAdapter;
import ai.humanitarians.pipeline.adapter.drive.LiveDriveAdapter;
import ai.humanitarians.pipeline.adapter.drive.MockDriveAdapter;
import ai.humanitarians.pipeline.adapter.youtube.LiveYouTubeAdapter;
import ai.humanitarians.pipeline.adapter.youtube.MockYouTubeAdapter;
import ai.humanitarians.pipeline.adapter.youtube.YouTubeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class AdapterConfiguration {

    private final PipelineProperties properties;

    public AdapterConfiguration(PipelineProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(name = "pipeline.use-mock-apis", havingValue = "true", matchIfMissing = true)
    public YouTubeAdapter mockYouTubeAdapter() {
        return new MockYouTubeAdapter(Path.of(".mock_youtube.json"));
    }

    @Bean
    @ConditionalOnProperty(name = "pipeline.use-mock-apis", havingValue = "false")
    public YouTubeAdapter liveYouTubeAdapter() {
        return new LiveYouTubeAdapter(properties.getYoutubeApiKey());
    }

    @Bean
    @ConditionalOnProperty(name = "pipeline.use-mock-apis", havingValue = "true", matchIfMissing = true)
    public DriveAdapter mockDriveAdapter() {
        return new MockDriveAdapter();
    }

    @Bean
    @ConditionalOnProperty(name = "pipeline.use-mock-apis", havingValue = "false")
    public DriveAdapter liveDriveAdapter() {
        return new LiveDriveAdapter(null);
    }
}
