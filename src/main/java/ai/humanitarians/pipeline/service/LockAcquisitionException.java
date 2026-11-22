package ai.humanitarians.pipeline.service;

public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String message) {
        super(message);
    }
}
