package com.llmscannerseguridad.application.project;

public final class ProjectDeletionException extends RuntimeException {
    public enum Reason { SCAN_ALREADY_ACTIVE, PROJECT_DELETE_CONFLICT }

    private final Reason reason;

    public ProjectDeletionException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
