package ai.humanitarians.pipeline.adapter.email;

import ai.humanitarians.pipeline.model.dto.EmailNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleEmailSink implements EmailSink {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailSink.class);

    @Override
    public boolean deliver(EmailNotification n) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("─".repeat(70)).append("\n");
        sb.append(String.format(" [DISPATCH SINK: CONSOLE] Mode: %s\n", n.mode()));
        sb.append(String.format("  To        : %s\n", n.recipientEmail()));
        if (!n.recipientEmail().equalsIgnoreCase(n.intendedRecipient())) {
            sb.append(String.format("  (Original): %s [REDIRECTED]\n", n.intendedRecipient()));
        }
        sb.append(String.format("  Subject   : %s\n", n.subject()));
        sb.append(String.format("  YouTube   : %s\n", n.youtubeUrl()));
        sb.append("─".repeat(70)).append("\n");
        sb.append(n.bodyText().strip()).append("\n");
        sb.append("─".repeat(70)).append("\n");

        System.out.println(sb);
        return true;
    }
}
