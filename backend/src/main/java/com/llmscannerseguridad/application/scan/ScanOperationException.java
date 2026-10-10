package com.llmscannerseguridad.application.scan;

import java.util.UUID;

public final class ScanOperationException extends RuntimeException {
    public enum Reason {
        PROJECT_NOT_FOUND, TARGET_NOT_CONFIGURED, SCAN_NOT_FOUND, SCAN_ALREADY_ACTIVE,
        SCAN_DISPATCH_FAILED, SCAN_EXECUTION_UNAVAILABLE, INTERNAL_ERROR
    }

    private final Reason reason;
    private final UUID projectId;
    private final UUID scanId;

    public ScanOperationException(Reason reason) {
        this(reason, null, null);
    }

    public ScanOperationException(Reason reason, UUID projectId, UUID scanId) {
        super(reason.name());
        this.reason = reason;
        this.projectId = projectId;
        this.scanId = scanId;
    }

    public Reason reason() { return reason; }
    public UUID projectId() { return projectId; }
    public UUID scanId() { return scanId; }

    public static ScanOperationException internal() {
        return new ScanOperationException(Reason.INTERNAL_ERROR);
    }
}
