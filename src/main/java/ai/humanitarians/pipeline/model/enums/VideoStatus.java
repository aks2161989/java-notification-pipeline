package ai.humanitarians.pipeline.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VideoStatus {
    SUBMITTED("submitted"),
    IN_REVIEW("in_review"),
    NEEDS_MODIFICATION("needs_modification"),
    QUEUED("queued"),
    PUBLISHED("published");

    private final String value;

    VideoStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static VideoStatus fromValue(String value) {
        if (value == null) return SUBMITTED;
        String normalized = value.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        for (VideoStatus status : values()) {
            if (status.value.equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown VideoStatus: " + value);
    }
}
