package com.llmscannerseguridad.domain.execution;

public enum ExecutionStatus {
    PENDING, RUNNING, COMPLETED, FAILED;

    public boolean active() {
        return this == PENDING || this == RUNNING;
    }
}
