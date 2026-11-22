package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.dto.EmailNotification;
import ai.humanitarians.pipeline.model.enums.PipelineMode;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EmailFormatter {

    private final PipelineProperties properties;

    public EmailFormatter(PipelineProperties properties) {
        this.properties = properties;
    }

    public EmailNotification render(
            Integer videoId,
            Integer fellowId,
            String fellowName,
            String fellowEmail,
            String videoTitle,
            String youtubeVideoId,
            String projectName,
            PipelineMode mode,
            String redirectTo
    ) {
        String youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeVideoId;

        // Recipient determination based on mode
        String actualRecipient = fellowEmail;
        if (mode == PipelineMode.REDIRECTED) {
            actualRecipient = (redirectTo != null && !redirectTo.isBlank())
                    ? redirectTo
                    : properties.getRedirectTestEmail();
        }

        // Subject line
        String subjectPrefix = "";
        if (mode == PipelineMode.DRY_RUN) {
            subjectPrefix = "[DRY-RUN] ";
        } else if (mode == PipelineMode.REDIRECTED) {
            subjectPrefix = "[TEST-REDIRECT] ";
        }
        String subject = String.format("%s🎉 Your video \"%s\" is now live on Humanitarians AI YouTube!",
                subjectPrefix, videoTitle);

        // Plain text version
        String bodyText = String.format("""
Hello %s,

Great news! Your video contribution for the %s project has completed editorial review and is now officially published on the Humanitarians AI YouTube channel.

Video Title: %s
Watch Link  : %s

Thank you for your dedication and impactful work. Feel free to share the link with your professional network and community!

Best regards,
Humanitarians AI Content & Production Team
https://humanitarians.ai
""", fellowName, projectName, videoTitle, youtubeUrl).strip();

        // Responsive HTML version
        String bodyHtml = String.format("""
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; padding: 20px; }
    .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }
    .header { background-color: #0f172a; color: #ffffff; padding: 24px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 600; letter-spacing: -0.5px; }
    .content { padding: 24px; line-height: 1.6; font-size: 15px; }
    .badge { display: inline-block; background-color: #dbeafe; color: #1e40af; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 12px; margin-bottom: 12px; }
    .video-card { background-color: #f1f5f9; border-left: 4px solid #2563eb; padding: 16px; margin: 20px 0; border-radius: 4px; }
    .btn { display: inline-block; background-color: #ef4444; color: #ffffff !important; text-decoration: none; font-weight: 600; padding: 12px 24px; border-radius: 6px; margin-top: 10px; }
    .footer { background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px 24px; font-size: 12px; color: #64748b; text-align: center; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>Humanitarians AI</h1>
    </div>
    <div class="content">
      <span class="badge">%s Fellow Contribution</span>
      <p>Hello <strong>%s</strong>,</p>
      <p>Your submitted video has successfully passed review and is now <strong>publicly live on YouTube</strong>!</p>
      
      <div class="video-card">
        <h3 style="margin: 0 0 8px 0; font-size: 16px; color: #0f172a;">%s</h3>
        <p style="margin: 0; font-size: 13px; color: #475569;">YouTube Video ID: <code>%s</code></p>
        <p style="margin-top: 12px;"><a href="%s" class="btn" target="_blank">▶ Watch on YouTube</a></p>
      </div>

      <p>Thank you for contributing to open humanitarian technology. You can now share this publication on LinkedIn and add it to your portfolio!</p>
    </div>
    <div class="footer">
      <p>Humanitarians AI Publication Notification Pipeline &bull; Automated Delivery</p>
      <p>Intended Fellow: %s &bull; Delivery Mode: %s</p>
    </div>
  </div>
</body>
</html>""", projectName, fellowName, videoTitle, youtubeVideoId, youtubeUrl, fellowEmail, mode.name());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Pipeline-Mode", mode.name());
        headers.put("X-Video-ID", String.valueOf(videoId));
        headers.put("X-Fellow-ID", String.valueOf(fellowId));
        headers.put("X-Original-Recipient", fellowEmail);

        return new EmailNotification(
                videoId, fellowId, fellowName, actualRecipient, fellowEmail,
                videoTitle, youtubeVideoId, youtubeUrl, projectName, subject,
                bodyText, bodyHtml, mode, headers, OffsetDateTime.now()
        );
    }
}
