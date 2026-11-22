package ai.humanitarians.pipeline.adapter.email;

import ai.humanitarians.pipeline.model.dto.EmailNotification;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.Map;

public class SmtpEmailSink implements EmailSink {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSink.class);
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSink(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress != null ? fromAddress : "notifications@humanitarians.ai";
    }

    @Override
    public boolean deliver(EmailNotification n) {
        if (mailSender == null) {
            throw new IllegalStateException("JavaMailSender not configured.");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, "Humanitarians AI");
            helper.setTo(n.recipientEmail());
            helper.setSubject(n.subject());
            helper.setText(n.bodyText(), n.bodyHtml());

            for (Map.Entry<String, String> entry : n.headers().entrySet()) {
                message.addHeader(entry.getKey(), entry.getValue());
            }

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMTP email for video {}: {}", n.videoId(), e.getMessage());
            throw new RuntimeException("SMTP delivery failed: " + e.getMessage(), e);
        }
    }
}
