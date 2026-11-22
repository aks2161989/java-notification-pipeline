package ai.humanitarians.pipeline.service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DataNormalizer {

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");
    private static final Pattern DRIVE_FILE_URL_PATTERN = Pattern.compile("/file/d/([a-zA-Z0-9_-]+)");
    private static final Pattern ZERO_WIDTH_PATTERN = Pattern.compile("[\\u200B\\u200E\\u200F\\uFEFF]");
    private static final Pattern EMBEDDED_YT_PATTERN = Pattern.compile("[a-zA-Z0-9_-]{11}");

    private DataNormalizer() {}

    public static String normalizeEmail(Object val) {
        if (val == null) return "";
        String clean = val.toString().trim().toLowerCase();
        clean = ZERO_WIDTH_PATTERN.matcher(clean).replaceAll("");
        return clean;
    }

    public static String extractYoutubeId(Object urlOrId) {
        if (urlOrId == null) return null;
        String raw = urlOrId.toString().trim();
        if (raw.isEmpty()) return null;

        // Case 1: Already an 11-char ID
        if (YOUTUBE_ID_PATTERN.matcher(raw).matches()) {
            return raw;
        }

        // Case 2: Parse as URL
        try {
            URI uri = URI.create(raw);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";

            if (host.contains("youtu.be")) {
                String path = uri.getPath();
                if (path != null) {
                    String[] parts = path.replaceAll("^/+", "").split("/");
                    if (parts.length > 0 && YOUTUBE_ID_PATTERN.matcher(parts[0]).matches()) {
                        return parts[0];
                    }
                }
            }

            if (host.contains("youtube.com")) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2 && "v".equalsIgnoreCase(pair[0])) {
                            String vid = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            if (YOUTUBE_ID_PATTERN.matcher(vid).matches()) {
                                return vid;
                            }
                        }
                    }
                }

                String path = uri.getPath();
                if (path != null) {
                    String[] parts = path.replaceAll("^/+", "").split("/");
                    if (parts.length >= 2 && ("embed".equalsIgnoreCase(parts[0]) || "v".equalsIgnoreCase(parts[0]))) {
                        if (YOUTUBE_ID_PATTERN.matcher(parts[1]).matches()) {
                            return parts[1];
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fallback to regex
        }

        // Case 3: Embedded fallback in string
        if (raw.contains("watch?v=") || raw.contains("youtu.be/")) {
            Matcher m = EMBEDDED_YT_PATTERN.matcher(raw);
            if (m.find()) {
                return m.group(0);
            }
        }

        return raw;
    }

    public static String extractDriveFileId(Object urlOrId) {
        if (urlOrId == null) return null;
        String raw = urlOrId.toString().trim();
        if (raw.isEmpty()) return null;

        // Case 1: /file/d/<id>/...
        Matcher m = DRIVE_FILE_URL_PATTERN.matcher(raw);
        if (m.find()) {
            return m.group(1);
        }

        // Case 2: drive.google.com/open?id=<id>
        try {
            URI uri = URI.create(raw);
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if (host.contains("drive.google.com")) {
                String query = uri.getQuery();
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=", 2);
                        if (pair.length == 2 && "id".equalsIgnoreCase(pair[0])) {
                            return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Case 3: Already raw ID
        return raw;
    }

    public static String normalizeStatus(Object val, String defaultValue) {
        if (val == null) return defaultValue;
        String s = val.toString().trim().toLowerCase().replace("-", "_").replace(" ", "_");
        return s.isEmpty() ? defaultValue : s;
    }

    public static String cleanText(Object val) {
        if (val == null) return null;
        String s = val.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
