package com.llmscannerseguridad.domain.evaluation;

public enum EvaluationStatus {
    PENDING, RUNNING, COMPLETED, FAILED;
    public boolean active() { return this == PENDING || this == RUNNING; }
}
