package ai.humanitarians.pipeline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EventType {
    SUBMITTED("submitted"),
    REVIEW_STARTED("review_started"),
    MODIFICATION_REQUESTED("modification_requested"),
    RESUBMITTED("resubmitted"),
    APPROVED("approved"),
    QUEUED("queued"),
    PUBLISHED("published");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EventType fromValue(String value) {
        if (value == null) return SUBMITTED;
        String normalized = value.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        for (EventType type : values()) {
            if (type.value.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown EventType: " + value);
    }
}
