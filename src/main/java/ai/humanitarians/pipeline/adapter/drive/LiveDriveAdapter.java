package ai.humanitarians.pipeline.adapter.drive;

import ai.humanitarians.pipeline.model.dto.DriveFileMetadata;
import com.google.api.services.drive.Drive;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LiveDriveAdapter implements DriveAdapter {

    private final Drive driveService;

    public LiveDriveAdapter(Drive driveService) {
        this.driveService = driveService;
    }

    @Override
    public Optional<DriveFileMetadata> getFileMetadata(String fileId) {
        if (driveService == null) {
            throw new UnsupportedOperationException("Live Google Drive client requires credentials.");
        }
        return Optional.empty();
    }

    @Override
    public List<DriveFileMetadata> listFolderSubmissions(String folderId) {
        if (driveService == null) {
            throw new UnsupportedOperationException("Live Google Drive client requires credentials.");
        }
        return Collections.emptyList();
    }
}
