package com.llmscannerseguridad.adapters.in.rest.scan;

public final class InvalidScanRequestException extends RuntimeException {
    public enum Reason { INVALID_PROJECT_ID, INVALID_SCAN_ID, INVALID_REQUEST }
    private final Reason reason;

    public InvalidScanRequestException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
