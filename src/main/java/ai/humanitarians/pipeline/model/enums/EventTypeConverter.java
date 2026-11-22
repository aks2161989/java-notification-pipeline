package ai.humanitarians.pipeline.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EventTypeConverter implements AttributeConverter<EventType, String> {
    @Override
    public String convertToDatabaseColumn(EventType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public EventType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EventType.fromValue(dbData);
    }
}
