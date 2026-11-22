package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.config.PipelineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutionLockService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLockService.class);

    private final Path defaultLockPath;
    private final int defaultTimeoutSeconds;

    public ExecutionLockService(PipelineProperties properties) {
        this.defaultLockPath = Path.of(properties.getLockFilePath());
        this.defaultTimeoutSeconds = properties.getLockTimeoutSeconds();
    }

    public LockHandle acquire() {
        return acquire(defaultLockPath, defaultTimeoutSeconds);
    }

    public LockHandle acquire(Path lockFile, int timeoutSeconds) {
        long now = Instant.now().getEpochSecond();
        long pid = ProcessHandle.current().pid();
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostname = "localhost";
        }

        if (Files.exists(lockFile)) {
            try {
                List<String> lines = Files.readAllLines(lockFile, StandardCharsets.UTF_8);
                Map<String, String> data = new HashMap<>();
                for (String line : lines) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        data.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                }

                double lockTimeDouble = Double.parseDouble(data.getOrDefault("timestamp", "0"));
                long lockTime = (long) lockTimeDouble;
                String lockPid = data.getOrDefault("pid", "unknown");
                String lockHost = data.getOrDefault("hostname", "unknown");
                String isoTime = data.getOrDefault("iso_time", "unknown");

                long age = now - lockTime;
                if (age < timeoutSeconds) {
                    throw new LockAcquisitionException(
                            String.format("Active execution lock held by PID %s on %s since %s (age: %ds < TTL: %ds). " +
                                    "Aborting run to prevent concurrent duplicate processing.",
                                    lockPid, lockHost, isoTime, age, timeoutSeconds)
                    );
                } else {
                    log.warn("Reclaiming stale lock (age: {}s > TTL: {}s) left by PID {}", age, timeoutSeconds, lockPid);
                }
            } catch (LockAcquisitionException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Corrupt lock file detected; reclaiming: {}", e.getMessage());
            }
        }

        // Write new lock payload
        String payload = String.format("pid=%d\nhostname=%s\ntimestamp=%d.0\niso_time=%s\n",
                pid, hostname, now, OffsetDateTime.now());
        try {
            if (lockFile.getParent() != null) {
                Files.createDirectories(lockFile.getParent());
            }
            Files.writeString(lockFile, payload, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write execution lock file: " + lockFile, e);
        }

        return new LockHandle(lockFile);
    }

    public static class LockHandle implements AutoCloseable {
        private final Path lockFile;
        private boolean active = true;

        public LockHandle(Path lockFile) {
            this.lockFile = lockFile;
        }

        public void release() {
            if (active) {
                try {
                    Files.deleteIfExists(lockFile);
                } catch (IOException ignored) {}
                active = false;
            }
        }

        @Override
        public void close() {
            release();
        }
    }
}
