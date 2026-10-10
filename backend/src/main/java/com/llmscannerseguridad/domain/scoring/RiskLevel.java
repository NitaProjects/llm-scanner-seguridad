package com.llmscannerseguridad.domain.scoring;

public enum RiskLevel {
    NO_FINDINGS, LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel fromScore(int score) {
        if (score < 0 || score > 100) throw new IllegalArgumentException("Score fuera de rango.");
        if (score == 0) return NO_FINDINGS;
        if (score < 40) return LOW;
        if (score < 70) return MEDIUM;
        if (score < 90) return HIGH;
        return CRITICAL;
    }
}
