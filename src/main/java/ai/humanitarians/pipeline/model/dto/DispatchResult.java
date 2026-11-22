package ai.humanitarians.pipeline.model.dto;

import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import java.time.OffsetDateTime;

public record DispatchResult(
        boolean success,
        NotificationDeliveryStatus deliveryStatus,
        String actualRecipient,
        String reason,
        OffsetDateTime sentOn
) {}
