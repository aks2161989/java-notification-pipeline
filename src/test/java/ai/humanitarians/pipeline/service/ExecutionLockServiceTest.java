package ai.humanitarians.pipeline.service;

import ai.humanitarians.pipeline.config.PipelineProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionLockServiceTest {

    @Test
    @DisplayName("Execution lock enforces mutual exclusion across concurrent runners")
    void testExecutionLockMutualExclusion(@TempDir Path tempDir) {
        Path lockFile = tempDir.resolve(".test.lock");
        PipelineProperties props = new PipelineProperties();
        props.setLockFilePath(lockFile.toString());
        props.setLockTimeoutSeconds(60);

        ExecutionLockService lockService = new ExecutionLockService(props);

        // First worker acquires
        ExecutionLockService.LockHandle handle1 = lockService.acquire(lockFile, 60);
        assertNotNull(handle1);
        assertTrue(Files.exists(lockFile));

        // Second worker attempts acquisition while active -> must throw LockAcquisitionException
        LockAcquisitionException ex = assertThrows(LockAcquisitionException.class, () ->
                lockService.acquire(lockFile, 60));
        assertTrue(ex.getMessage().contains("Active execution lock held"));

        // First worker releases
        handle1.release();
        assertFalse(Files.exists(lockFile));

        // Second worker can now acquire
        ExecutionLockService.LockHandle handle2 = lockService.acquire(lockFile, 60);
        assertNotNull(handle2);
        handle2.release();
    }

    @Test
    @DisplayName("Stale execution lock exceeding TTL is safely reclaimed")
    void testExecutionLockStaleReclaim(@TempDir Path tempDir) throws IOException {
        Path lockFile = tempDir.resolve(".stale.lock");
        // Write simulated crashed process lock payload with old timestamp
        String stalePayload = "pid=99999\nhostname=crashed-host\ntimestamp=1000.0\niso_time=2020-01-01T00:00:00Z\n";
        Files.writeString(lockFile, stalePayload);

        PipelineProperties props = new PipelineProperties();
        ExecutionLockService lockService = new ExecutionLockService(props);

        // Runner with 10s timeout should detect stale lock and reclaim
        ExecutionLockService.LockHandle handle = lockService.acquire(lockFile, 10);
        assertNotNull(handle);
        assertTrue(Files.exists(lockFile));
        handle.release();
        assertFalse(Files.exists(lockFile));
    }
}
