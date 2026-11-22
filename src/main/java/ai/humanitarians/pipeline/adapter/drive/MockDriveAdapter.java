package ai.humanitarians.pipeline.adapter.drive;

import ai.humanitarians.pipeline.model.dto.DriveFileMetadata;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockDriveAdapter implements DriveAdapter {

    private final Map<String, DriveFileMetadata> files = new ConcurrentHashMap<>();

    public MockDriveAdapter() {
        OffsetDateTime now = OffsetDateTime.now();
        files.put("drive_file_101", new DriveFileMetadata(
                "drive_file_101",
                "Madison_Architecture_Final.mp4",
                "video/mp4",
                450_000_000L,
                now,
                "/Submissions/Madison/Jainil_Shah",
                "shah.jainil@northeastern.edu"
        ));
        files.put("drive_file_102", new DriveFileMetadata(
                "drive_file_102",
                "NeuroVEP_Analysis_v2.mp4",
                "video/mp4",
                320_000_000L,
                now,
                "/Submissions/NeuroVEP/Maya_Lin",
                "m.lin@northeastern.edu"
        ));
    }

    public void registerFile(DriveFileMetadata file) {
        files.put(file.fileId(), file);
    }

    @Override
    public Optional<DriveFileMetadata> getFileMetadata(String fileId) {
        return Optional.ofNullable(files.get(fileId));
    }

    @Override
    public List<DriveFileMetadata> listFolderSubmissions(String folderId) {
        return new ArrayList<>(files.values());
    }
}
