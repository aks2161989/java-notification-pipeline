package ai.humanitarians.pipeline.model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VideoStatusConverter implements AttributeConverter<VideoStatus, String> {
    @Override
    public String convertToDatabaseColumn(VideoStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public VideoStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VideoStatus.fromValue(dbData);
    }
}
