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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
        if (!Files.exists(xlsxPath)) {
            log.warn("Excel file does not exist at path: {}", xlsxPath);
            return Map.of("projects", 0, "pms", 0, "fellows", 0, "videos", 0);
        }

        try (InputStream is = Files.newInputStream(xlsxPath);
             Workbook workbook = new XSSFWorkbook(is)) {
            return seedFromWorkbook(workbook);
        } catch (Exception e) {
            log.error("Failed to seed from Excel {}: {}", xlsxPath, e.getMessage(), e);
            return Map.of("error", 1);
        }
    }

    @Transactional
    public Map<String, Integer> syncFromGoogleSheetWorkbook() {
        return syncFromGoogleSheetWorkbook(properties.getTrackerWorkbookUrl());
    }

    @Transactional
    public Map<String, Integer> syncFromGoogleSheetWorkbook(String workbookUrl) {
        log.info("Fetching complete tracker workbook from Google Sheets: {}", workbookUrl);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(workbookUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch Google Sheet workbook: HTTP {}", response.statusCode());
                return Map.of("error", response.statusCode());
            }
            try (InputStream is = response.body();
                 Workbook workbook = new XSSFWorkbook(is)) {
                return seedFromWorkbook(workbook);
            }
        } catch (Exception e) {
            log.error("Failed to sync from Google Sheet workbook {}: {}", workbookUrl, e.getMessage(), e);
            throw new RuntimeException("Google Sheet workbook sync failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Integer> seedFromWorkbook(Workbook workbook) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("projects", 0);
        counts.put("pms", 0);
        counts.put("fellows", 0);
        counts.put("videos", 0);

        try {
            // 1. Projects Sheet
            Sheet projectsSheet = findSheetIgnoreCase(workbook, "projects");
            if (projectsSheet != null) {
                Map<String, Integer> pCols = buildHeaderMap(projectsSheet);
                for (Row row : projectsSheet) {
                    if (row.getRowNum() == 0) continue;
                    int idCol = pCols.getOrDefault("project_id", 0);
                    Integer pid = getCellInt(row.getCell(idCol), null);
                    if (pid == null) continue;
                    String name = getCellString(row.getCell(pCols.getOrDefault("project_name", 1)), "Project " + pid);
                    String type = getCellString(row.getCell(pCols.getOrDefault("project_type", 2)), "internal");

                    Project p = projectRepository.findById(pid).orElseGet(() -> new Project(pid, name, type));
                    p.setProjectName(name);
                    p.setProjectType(type);
                    projectRepository.save(p);
                    counts.put("projects", counts.get("projects") + 1);
                }
            }

            // 2. PMs Sheet
            Sheet pmsSheet = findSheetIgnoreCase(workbook, "pms");
            if (pmsSheet != null) {
                Map<String, Integer> pmCols = buildHeaderMap(pmsSheet);
                for (Row row : pmsSheet) {
                    if (row.getRowNum() == 0) continue;
                    int idCol = pmCols.getOrDefault("pm_id", 0);
                    Integer pmid = getCellInt(row.getCell(idCol), null);
                    if (pmid == null) continue;
                    int pid = getCellInt(row.getCell(pmCols.getOrDefault("project_id", 1)), 1);
                    String name = DataNormalizer.cleanText(getCellString(row.getCell(pmCols.getOrDefault("full_name", 2)), "PM " + pmid));
                    String email = DataNormalizer.normalizeEmail(getCellString(row.getCell(pmCols.getOrDefault("email", 3)), ""));
                    String role = DataNormalizer.cleanText(getCellString(row.getCell(pmCols.getOrDefault("role", 4)), "pm"));
                    String status = DataNormalizer.normalizeStatus(getCellString(row.getCell(pmCols.getOrDefault("status", 5)), "active"), "active");

                    Project p = projectRepository.findById(pid).orElseGet(() ->
                            projectRepository.save(new Project(pid, "Project " + pid, "internal")));
                    PM pm = pmRepository.findById(pmid).orElseGet(() -> new PM(pmid, p, name, email, role, status));
                    pm.setProject(p);
                    pm.setFullName(name);
                    pm.setEmail(email);
                    pm.setRole(role);
                    pm.setStatus(status);
                    pmRepository.save(pm);
                    counts.put("pms", counts.get("pms") + 1);
                }
            }

            // 3. Fellows Sheet (Check "Fellow Emails" first for the full roster, then fallback or supplement with "fellows")
            List<Sheet> fellowSheets = new ArrayList<>();
            Sheet fellowEmailsSheet = findSheetIgnoreCase(workbook, "Fellow Emails", "fellow emails");
            if (fellowEmailsSheet != null) fellowSheets.add(fellowEmailsSheet);
            Sheet legacyFellowsSheet = findSheetIgnoreCase(workbook, "fellows");
            if (legacyFellowsSheet != null && legacyFellowsSheet != fellowEmailsSheet) {
                fellowSheets.add(legacyFellowsSheet);
            }

            for (Sheet sheet : fellowSheets) {
                Map<String, Integer> fCols = buildHeaderMap(sheet);
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue;

                    if (fCols.containsKey("fellow_id") || (fCols.containsKey("name") && fCols.containsKey("email")) || fCols.containsKey("email")) {
                        String name = fCols.containsKey("full_name")
                                ? DataNormalizer.cleanText(getCellString(row.getCell(fCols.get("full_name")), "Fellow"))
                                : (fCols.containsKey("name") ? DataNormalizer.cleanText(getCellString(row.getCell(fCols.get("name")), "Fellow")) : "Fellow");
                        String email = fCols.containsKey("email") ? DataNormalizer.normalizeEmail(getCellString(row.getCell(fCols.get("email")), "")) : "";
                        if (email.isBlank() || !email.contains("@")) continue;

                        String projName = fCols.containsKey("project") ? getCellString(row.getCell(fCols.get("project")), null) : null;
                        int pid = fCols.containsKey("project_id") ? getCellInt(row.getCell(fCols.get("project_id")), 1) : 1;
                        String status = fCols.containsKey("status") ? DataNormalizer.normalizeStatus(getCellString(row.getCell(fCols.get("status")), "active"), "active") : "active";
                        String label = fCols.containsKey("display_label") ? DataNormalizer.cleanText(getCellString(row.getCell(fCols.get("display_label")), null)) : name;

                        Project p;
                        if (projName != null && !projName.isBlank()) {
                            String pName = projName;
                            p = projectRepository.findAll().stream()
                                    .filter(proj -> proj.getProjectName().equalsIgnoreCase(pName))
                                    .findFirst()
                                    .orElseGet(() -> {
                                        int nextPid = (int) (projectRepository.count() + 1);
                                        while (projectRepository.existsById(nextPid)) nextPid++;
                                        return projectRepository.save(new Project(nextPid, pName, "internal"));
                                    });
                        } else {
                            p = projectRepository.findById(pid).orElseGet(() ->
                                    projectRepository.save(new Project(pid, "General / Unassigned", "internal")));
                        }

                        Optional<Fellow> existingOpt = fellowRepository.findByEmail(email);
                        if (existingOpt.isPresent()) {
                            Fellow f = existingOpt.get();
                            f.setProject(p);
                            f.setFullName(name);
                            f.setStatus(status);
                            f.setDisplayLabel(label != null ? label : name);
                            fellowRepository.save(f);
                        } else {
                            Integer fid = fCols.containsKey("fellow_id") ? getCellInt(row.getCell(fCols.get("fellow_id")), null) : null;
                            if (fid == null || fellowRepository.existsById(fid)) {
                                fid = (int) (fellowRepository.count() + 1);
                                while (fellowRepository.existsById(fid)) fid++;
                            }
                            Fellow f = new Fellow(fid, p, name, email, status, label != null ? label : name);
                            fellowRepository.save(f);
                        }
                        counts.put("fellows", counts.get("fellows") + 1);
                    }
                }
            }

            // 4. Videos Sheet
            Sheet videosSheet = findSheetIgnoreCase(workbook, "videos");
            if (videosSheet != null) {
                Map<String, Integer> vCols = buildHeaderMap(videosSheet);
                for (Row row : videosSheet) {
                    if (row.getRowNum() == 0) continue;
                    int idCol = vCols.getOrDefault("video_id", 0);
                    Integer vid = getCellInt(row.getCell(idCol), null);
                    if (vid == null) continue;

                    // Resolve fellow_id: check fellow_id column, fallback to fellow label/email/col 1
                    Integer fid = null;
                    if (vCols.containsKey("fellow_id")) {
                        fid = getCellInt(row.getCell(vCols.get("fellow_id")), null);
                    }
                    if (fid == null && vCols.containsKey("fellow")) {
                        String fLabel = getCellString(row.getCell(vCols.get("fellow")), null);
                        if (fLabel != null && !fLabel.isBlank()) {
                            String cleanLabel = DataNormalizer.cleanText(fLabel);
                            fid = fellowRepository.findByEmail(cleanLabel)
                                    .or(() -> fellowRepository.findAll().stream()
                                            .filter(fel -> (fel.getDisplayLabel() != null && fel.getDisplayLabel().equalsIgnoreCase(cleanLabel))
                                                    || (fel.getFullName() != null && fel.getFullName().equalsIgnoreCase(cleanLabel))
                                                    || (fel.getFullName() != null && fel.getFullName().toLowerCase().startsWith(cleanLabel.toLowerCase())))
                                            .findFirst())
                                    .map(Fellow::getFellowId).orElse(null);
                        }
                    }
                    if (fid == null && row.getCell(1) != null && row.getCell(1).getCellType() == CellType.NUMERIC) {
                        fid = (int) row.getCell(1).getNumericCellValue();
                    }

                    int titleCol = vCols.getOrDefault("title", 2);
                    String title = DataNormalizer.cleanText(getCellString(row.getCell(titleCol), "Video " + vid));

                    int ytCol = vCols.containsKey("youtube_link") ? vCols.get("youtube_link") : vCols.getOrDefault("youtube_video_id", 3);
                    String ytLink = getCellString(row.getCell(ytCol), null);
                    String ytId = DataNormalizer.extractYoutubeId(ytLink);
                    if (ytId == null && vCols.containsKey("youtube_video_id")) {
                        ytId = DataNormalizer.extractYoutubeId(getCellString(row.getCell(vCols.get("youtube_video_id")), null));
                    }

                    int driveCol = vCols.containsKey("drive_link") ? vCols.get("drive_link") : vCols.getOrDefault("drive_file_id", 4);
                    String driveLink = getCellString(row.getCell(driveCol), null);
                    String driveId = DataNormalizer.extractDriveFileId(driveLink);
                    if (driveId == null && vCols.containsKey("drive_file_id")) {
                        driveId = DataNormalizer.extractDriveFileId(getCellString(row.getCell(vCols.get("drive_file_id")), null));
                    }

                    int statusCol = vCols.containsKey("current_status") ? vCols.get("current_status") : vCols.getOrDefault("status", 6);
                    String statusStr = DataNormalizer.normalizeStatus(getCellString(row.getCell(statusCol), "submitted"), "submitted");

                    String statusNote = vCols.containsKey("status_note") ? DataNormalizer.cleanText(getCellString(row.getCell(vCols.get("status_note")), null)) : null;

                    OffsetDateTime submittedOn = vCols.containsKey("submitted_on") ? getCellDateTime(row.getCell(vCols.get("submitted_on"))) : null;
                    OffsetDateTime publishedOn = vCols.containsKey("published_on") ? getCellDateTime(row.getCell(vCols.get("published_on"))) : null;

                    Fellow f = (fid != null) ? fellowRepository.findById(fid).orElse(null) : null;

                    VideoRecord vr = videoRepository.findById(vid).orElseGet(VideoRecord::new);
                    vr.setVideoId(vid);
                    vr.setFellow(f);
                    vr.setTitle(title);
                    vr.setYoutubeLink(ytLink);
                    vr.setYoutubeVideoId(ytId);
                    vr.setDriveLink(driveLink);
                    vr.setDriveFileId(driveId);
                    VideoStatus newStatus = VideoStatus.fromValue(statusStr);
                    vr.setCurrentStatus(newStatus);
                    if (statusNote != null) {
                        vr.setStatusNote(statusNote);
                    }
                    if (submittedOn != null) {
                        vr.setSubmittedOn(submittedOn);
                    } else if (vr.getSubmittedOn() == null) {
                        vr.setSubmittedOn(OffsetDateTime.now());
                    }
                    if (publishedOn != null) {
                        vr.setPublishedOn(publishedOn);
                    }
                    if (newStatus == VideoStatus.QUEUED) {
                        notificationRepository.deleteByVideoId(vid);
                    }
                    videoRepository.save(vr);
                    counts.put("videos", counts.get("videos") + 1);
                }
            }

        } catch (Exception e) {
            log.error("Failed to seed from workbook: {}", e.getMessage(), e);
        }

        return counts;
    }

    @Transactional
    public Map<String, Integer> syncFellowsFromGoogleSheet() {
        return syncFellowsFromGoogleSheet(properties.getFellowsSheetUrl());
    }

    @Transactional
    public Map<String, Integer> syncFellowsFromGoogleSheet(String csvUrl) {
        log.info("Fetching fellow roster from Google Sheet: {}", csvUrl);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(csvUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch Google Sheet CSV: HTTP {}", response.statusCode());
                return Map.of("error", response.statusCode());
            }
            return syncFellowsFromCsv(response.body());
        } catch (Exception e) {
            log.error("Exception fetching fellow roster from Google Sheet: {}", e.getMessage(), e);
            throw new RuntimeException("Google Sheet fetch failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Map<String, Integer> syncFellowsFromCsv(String csvContent) {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("added", 0);
        counts.put("updated", 0);
        counts.put("deactivated", 0);
        counts.put("total_active", 0);

        if (csvContent == null || csvContent.isBlank()) {
            return counts;
        }

        // Ensure default project exists for fellows without an assigned project
        Project defaultProject = projectRepository.findAll().stream()
                .filter(p -> "General / Unassigned".equalsIgnoreCase(p.getProjectName()))
                .findFirst()
                .orElseGet(() -> {
                    int pid = 999;
                    while (projectRepository.existsById(pid)) pid++;
                    return projectRepository.save(new Project(pid, "General / Unassigned", "internal"));
                });

        Set<String> sheetEmails = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean isHeader = true;
            int nameCol = 0, emailCol = 1, projCol = 2;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);

                if (isHeader) {
                    isHeader = false;
                    for (int i = 0; i < parts.length; i++) {
                        String h = parts[i].trim().toLowerCase();
                        if (h.contains("name")) nameCol = i;
                        else if (h.contains("email")) emailCol = i;
                        else if (h.contains("project")) projCol = i;
                    }
                    continue;
                }

                if (parts.length <= emailCol) continue;

                String name = parts.length > nameCol ? DataNormalizer.cleanText(parts[nameCol]) : "Unknown Fellow";
                String email = DataNormalizer.normalizeEmail(parts[emailCol]);
                String projectName = (parts.length > projCol && parts[projCol] != null && !parts[projCol].isBlank())
                        ? DataNormalizer.cleanText(parts[projCol])
                        : null;

                if (email == null || email.isBlank() || !email.contains("@")) {
                    continue;
                }

                sheetEmails.add(email);

                // Resolve project
                Project project = defaultProject;
                if (projectName != null && !projectName.isBlank()) {
                    String pName = projectName;
                    project = projectRepository.findAll().stream()
                            .filter(p -> p.getProjectName().equalsIgnoreCase(pName))
                            .findFirst()
                            .orElseGet(() -> {
                                int pid = (int) (projectRepository.count() + 1);
                                while (projectRepository.existsById(pid)) pid++;
                                return projectRepository.save(new Project(pid, pName, "internal"));
                            });
                }

                Optional<Fellow> existingOpt = fellowRepository.findByEmail(email);
                if (existingOpt.isPresent()) {
                    Fellow existing = existingOpt.get();
                    existing.setFullName(name);
                    existing.setProject(project);
                    existing.setStatus("active");
                    existing.setDisplayLabel(name + (projectName != null ? " (" + projectName + ")" : ""));
                    fellowRepository.save(existing);
                    counts.put("updated", counts.get("updated") + 1);
                } else {
                    int nextFid = (int) (fellowRepository.count() + 1);
                    while (fellowRepository.existsById(nextFid)) nextFid++;
                    String label = name + (projectName != null ? " (" + projectName + ")" : "");
                    Fellow newFellow = new Fellow(nextFid, project, name, email, "active", label);
                    fellowRepository.save(newFellow);
                    counts.put("added", counts.get("added") + 1);
                }
            }

            // Reconciliation: Deactivate any fellow not present in the Google Sheet roster
            List<Fellow> allFellows = fellowRepository.findAll();
            for (Fellow f : allFellows) {
                if (f.getEmail() != null && !sheetEmails.contains(f.getEmail().toLowerCase())) {
                    if (!"inactive".equalsIgnoreCase(f.getStatus())) {
                        f.setStatus("inactive");
                        fellowRepository.save(f);
                        counts.put("deactivated", counts.get("deactivated") + 1);
                    }
                }
            }

            int totalActive = (int) fellowRepository.findAll().stream()
                    .filter(f -> "active".equalsIgnoreCase(f.getStatus()))
                    .count();
            counts.put("total_active", totalActive);

            log.info("Fellow roster synced from CSV: added={}, updated={}, deactivated={}, total_active={}",
                    counts.get("added"), counts.get("updated"), counts.get("deactivated"), totalActive);

        } catch (Exception e) {
            log.error("Failed to parse fellow CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Fellow CSV parsing failed: " + e.getMessage(), e);
        }

        return counts;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private Map<String, Integer> buildHeaderMap(Sheet sheet) {
        Map<String, Integer> map = new HashMap<>();
        if (sheet == null) return map;
        Row header = sheet.getRow(0);
        if (header == null) return map;
        for (Cell cell : header) {
            String val = getCellString(cell, "").trim().toLowerCase();
            if (!val.isEmpty()) {
                map.put(val, cell.getColumnIndex());
            }
        }
        return map;
    }

    private Integer getCellInt(Cell cell, Integer fallback) {
        if (cell == null) return fallback;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
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

    private Sheet findSheetIgnoreCase(Workbook workbook, String... names) {
        if (workbook == null || names == null) return null;
        for (String target : names) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).trim().equalsIgnoreCase(target.trim())) {
                    return workbook.getSheetAt(i);
                }
            }
        }
        return null;
    }

    private OffsetDateTime getCellDateTime(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date != null ? date.toInstant().atOffset(ZoneOffset.UTC) : null;
        }
        if (cell.getCellType() == CellType.STRING) {
            String str = cell.getStringCellValue().trim();
            if (str.isEmpty()) return null;
            try {
                return OffsetDateTime.parse(str);
            } catch (Exception e1) {
                try {
                    return java.time.LocalDate.parse(str).atStartOfDay().atOffset(ZoneOffset.UTC);
                } catch (Exception e2) {
                    try {
                        return java.time.LocalDateTime.parse(str).atOffset(ZoneOffset.UTC);
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }
}
