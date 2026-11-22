package ai.humanitarians.pipeline.adapter.email;

import ai.humanitarians.pipeline.model.dto.EmailNotification;

public interface EmailSink {
    boolean deliver(EmailNotification notification);
}
