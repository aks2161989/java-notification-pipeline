package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.config.PipelineProperties;
import ai.humanitarians.pipeline.model.entity.*;
import ai.humanitarians.pipeline.model.enums.EventType;
import ai.humanitarians.pipeline.model.enums.NotificationDeliveryStatus;
import ai.humanitarians.pipeline.model.enums.VideoStatus;
import ai.humanitarians.pipeline.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class DatabaseSeederService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeederService.class);

    private final ProjectRepository projectRepository;
    private final PMRepository pmRepository;
    private final FellowRepository fellowRepository;
    private final VideoRecordRepository videoRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final NotificationLogRepository notificationRepository;
    private final PipelineProperties properties;

    public DatabaseSeederService(
            ProjectRepository projectRepository,
            PMRepository pmRepository,
            FellowRepository fellowRepository,
            VideoRecordRepository videoRepository,
            ReviewEventRepository reviewEventRepository,
            NotificationLogRepository notificationRepository,
            PipelineProperties properties
    ) {
        this.projectRepository = projectRepository;
        this.pmRepository = pmRepository;
        this.fellowRepository = fellowRepository;
        this.videoRepository = videoRepository;
        this.reviewEventRepository = reviewEventRepository;
        this.notificationRepository = notificationRepository;
        this.properties = properties;
    }

    @Transactional
    public Map<String, Integer> seedSyntheticScenarios() {
        Map<String, Integer> counts = new HashMap<>();

        // 1. Projects
        Project p1 = projectRepository.save(new Project(1, "Madison", "internal"));
        Project p2 = projectRepository.save(new Project(2, "NeuroVEP", "internal"));
        Project p3 = projectRepository.save(new Project(3, "Lyrical Literacy", "external"));
        Project p4 = projectRepository.save(new Project(4, "Medhavi", "research"));
        counts.put("projects", 4);

        // 2. PMs
        pmRepository.save(new PM(1, p1, "Professor Brown", "brown@humanitarians.ai", "lead_reviewer", "active"));
        pmRepository.save(new PM(2, p2, "Nina Editorial", "nina@humanitarians.ai", "editorial_lead", "active"));
        counts.put("pms", 2);

        // 3. Fellows
        Fellow f1 = fellowRepository.save(new Fellow(1, p1, "Jainil Shah", "shah.jainil@northeastern.edu", "active", "Jainil Shah (Madison)"));
        Fellow f2 = fellowRepository.save(new Fellow(2, p2, "Maya Lin", "m.lin@northeastern.edu", "active", "Maya Lin (NeuroVEP)"));
        Fellow f3 = fellowRepository.save(new Fellow(3, p3, "Marcus Vance", "m.vance@northeastern.edu", "active", "Marcus Vance (Lyrical)"));
        Fellow f4 = fellowRepository.save(new Fellow(4, p4, "Priya Sharma", "p.sharma@northeastern.edu", "active", "Priya Sharma (Medhavi)"));
        Fellow f5 = fellowRepository.save(new Fellow(5, p4, "Inactive Fellow", "inactive@northeastern.edu", "inactive", "Inactive Fellow"));
        counts.put("fellows", 5);

        // 4. Videos
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        VideoRecord v1 = new VideoRecord(1, f1, "Madison System Architecture Overview", "RzW17zhV4Nw", VideoStatus.QUEUED);
        v1.setDriveFileId("drive_file_101");
        v1.setDriveLink("https://drive.google.com/file/d/drive_file_101/view");
        v1.setYoutubeLink("https://www.youtube.com/watch?v=RzW17zhV4Nw");
        v1.setSubmittedOn(now.minusDays(5));
        videoRepository.save(v1);

        VideoRecord v2 = new VideoRecord(2, f2, "NeuroVEP Visual Evoked Potential Analysis", "dQw4w9WgXcQ", VideoStatus.QUEUED);
        v2.setDriveFileId("drive_file_102");
        v2.setDriveLink("https://drive.google.com/file/d/drive_file_102/view");
        v2.setSubmittedOn(now.minusDays(3));
        videoRepository.save(v2);

        VideoRecord v3 = new VideoRecord(3, f3, "Lyrical Literacy Interactive Learning Demo", "3C_zAB12345", VideoStatus.PUBLISHED);
        v3.setDriveFileId("drive_file_103");
        v3.setSubmittedOn(now.minusDays(7));
        v3.setPublishedOn(now.minusDays(1));
        videoRepository.save(v3);

        VideoRecord v4 = new VideoRecord(4, f4, "Medhavi Assistive Tech Walkthrough", "4D_wCD67890", VideoStatus.QUEUED);
        v4.setDriveFileId("drive_file_104");
        v4.setSubmittedOn(now.minusDays(2));
        videoRepository.save(v4);

        VideoRecord v5 = new VideoRecord(5, f1, "Caching Explained in 5 Minutes", "5E_vEF99999", VideoStatus.NEEDS_MODIFICATION);
        v5.setDriveFileId("drive_file_105");
        v5.setSubmittedOn(now.minusDays(4));
        videoRepository.save(v5);

        counts.put("videos", 5);

        // 5. Review Events
        reviewEventRepository.save(new ReviewEvent(v1, EventType.SUBMITTED, "fellow_uploader", "Initial Drive upload", now.minusDays(5)));
        reviewEventRepository.save(new ReviewEvent(v1, EventType.QUEUED, "prof.brown", "Passed review, moved to Queue", now.minusDays(2)));

        reviewEventRepository.save(new ReviewEvent(v3, EventType.SUBMITTED, "fellow_uploader", "Uploaded to Drive", now.minusDays(7)));
        reviewEventRepository.save(new ReviewEvent(v3, EventType.APPROVED, "nina", "Approved for public release", now.minusDays(3)));
        reviewEventRepository.save(new ReviewEvent(v3, EventType.PUBLISHED, "youtube_poller", "Published to YouTube channel", now.minusDays(1)));
        counts.put("review_events", 5);

        // 6. Existing Notification for Video 3 (sent)
        NotificationLog n3 = new NotificationLog(v3, f3.getEmail(), NotificationDeliveryStatus.SENT, "Video published on YouTube (3C_zAB12345)");
        n3.setSentOn(now.minusDays(1));
        notificationRepository.save(n3);
        counts.put("notifications", 1);

        log.info("Synthetic scenarios successfully seeded: {}", counts);
        return counts;
    }

    @Transactional
    public Map<String, Integer> seedFromExcel(Path xlsxPath) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("projects", 0);
        counts.put("pms", 0);
        counts.put("fellows", 0);
        counts.put("videos", 0);

        if (!Files.exists(xlsxPath)) {
            log.warn("Excel file does not exist at path: {}", xlsxPath);
            return counts;
        }

        try (InputStream is = Files.newInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(is)) {

            // 1. Projects Sheet
            Sheet projectsSheet = workbook.getSheet("projects");
            if (projectsSheet != null) {
                for (Row row : projectsSheet) {
                    if (row.getRowNum() == 0) continue;
                    Cell idCell = row.getCell(0);
                    if (idCell == null || idCell.getCellType() == CellType.BLANK) continue;
                    int pid = (int) idCell.getNumericCellValue();
                    String name = getCellString(row.getCell(1), "Project " + pid);
                    String type = getCellString(row.getCell(2), "internal");
                    if (!projectRepository.existsById(pid)) {
                        projectRepository.save(new Project(pid, name, type));
                        counts.put("projects", counts.get("projects") + 1);
                    }
                }
            }

            // 2. Fellows Sheet
            Sheet fellowsSheet = workbook.getSheet("fellows");
            if (fellowsSheet != null) {
                for (Row row : fellowsSheet) {
                    if (row.getRowNum() == 0) continue;
                    Cell idCell = row.getCell(0);
                    if (idCell == null || idCell.getCellType() == CellType.BLANK) continue;
                    int fid = (int) idCell.getNumericCellValue();
                    int pid = (int) (row.getCell(1) != null ? row.getCell(1).getNumericCellValue() : 1);
                    String name = DataNormalizer.cleanText(getCellString(row.getCell(2), "Fellow " + fid));
                    String email = DataNormalizer.normalizeEmail(getCellString(row.getCell(3), ""));
                    String status = DataNormalizer.normalizeStatus(getCellString(row.getCell(4), "active"), "active");
                    String label = DataNormalizer.cleanText(getCellString(row.getCell(5), null));

                    if (!fellowRepository.existsById(fid)) {
                        Project p = projectRepository.findById(pid).orElseGet(() ->
                                projectRepository.save(new Project(pid, "Project " + pid, "internal")));
                        fellowRepository.save(new Fellow(fid, p, name, email, status, label));
                        counts.put("fellows", counts.get("fellows") + 1);
                    }
                }
            }

            // 3. Videos Sheet
            Sheet videosSheet = workbook.getSheet("videos");
            if (videosSheet != null) {
                for (Row row : videosSheet) {
                    if (row.getRowNum() == 0) continue;
                    Cell idCell = row.getCell(0);
                    if (idCell == null || idCell.getCellType() == CellType.BLANK) continue;
                    int vid = (int) idCell.getNumericCellValue();
                    Integer fid = row.getCell(1) != null && row.getCell(1).getCellType() == CellType.NUMERIC
                            ? (int) row.getCell(1).getNumericCellValue() : null;
                    String title = DataNormalizer.cleanText(getCellString(row.getCell(2), "Video " + vid));
                    String ytLink = getCellString(row.getCell(3), null);
                    String driveLink = getCellString(row.getCell(4), null);
                    String statusStr = DataNormalizer.normalizeStatus(getCellString(row.getCell(5), "submitted"), "submitted");

                    if (!videoRepository.existsById(vid)) {
                        Fellow f = (fid != null) ? fellowRepository.findById(fid).orElse(null) : null;
                        VideoRecord vr = new VideoRecord(vid, f, title, DataNormalizer.extractYoutubeId(ytLink),
                                VideoStatus.fromValue(statusStr));
                        vr.setYoutubeLink(ytLink);
                        vr.setDriveLink(driveLink);
                        vr.setDriveFileId(DataNormalizer.extractDriveFileId(driveLink));
                        vr.setSubmittedOn(OffsetDateTime.now());
                        videoRepository.save(vr);
                        counts.put("videos", counts.get("videos") + 1);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to seed from Excel {}: {}", xlsxPath, e.getMessage(), e);
        }

        return counts;
    }

    private String getCellString(Cell cell, String fallback) {
        if (cell == null) return fallback;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> fallback;
        };
    }
}
