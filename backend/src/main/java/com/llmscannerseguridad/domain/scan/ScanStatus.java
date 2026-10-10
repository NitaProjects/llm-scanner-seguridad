package com.llmscannerseguridad.domain.scan;

public enum ScanStatus {
    PENDING, RUNNING, COMPLETED, FAILED;

    public boolean active() {
        return this == PENDING || this == RUNNING;
    }
}
