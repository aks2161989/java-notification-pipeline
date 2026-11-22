package ai.humanitarians.pipeline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationDeliveryStatus {
    PENDING("pending"),
    SENDING("sending"),
    SENT("sent"),
    FAILED("failed"),
    SKIPPED("skipped");

    private final String value;

    NotificationDeliveryStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NotificationDeliveryStatus fromValue(String value) {
        if (value == null) return PENDING;
        String normalized = value.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        for (NotificationDeliveryStatus status : values()) {
            if (status.value.equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationDeliveryStatus: " + value);
    }
}
