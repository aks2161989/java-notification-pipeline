package ai.humanitarians.pipeline.adapter.drive;

import ai.humanitarians.pipeline.model.dto.DriveFileMetadata;
import java.util.List;
import java.util.Optional;

public interface DriveAdapter {
    Optional<DriveFileMetadata> getFileMetadata(String fileId);
    List<DriveFileMetadata> listFolderSubmissions(String folderId);
}
