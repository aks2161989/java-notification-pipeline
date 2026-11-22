package ai.humanitarians.pipeline.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationDeliveryStatusConverter implements AttributeConverter<NotificationDeliveryStatus, String> {
    @Override
    public String convertToDatabaseColumn(NotificationDeliveryStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public NotificationDeliveryStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationDeliveryStatus.fromValue(dbData);
    }
}
