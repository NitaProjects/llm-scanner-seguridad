package com.llmscannerseguridad.domain.scoring;

public record SeverityCounts(int LOW, int MEDIUM, int HIGH, int CRITICAL) {
    public SeverityCounts {
        if (LOW < 0 || MEDIUM < 0 || HIGH < 0 || CRITICAL < 0) throw new IllegalArgumentException("Contadores no válidos.");
        Math.addExact(Math.addExact(LOW, MEDIUM), Math.addExact(HIGH, CRITICAL));
    }
    public int total() { return Math.addExact(Math.addExact(LOW, MEDIUM), Math.addExact(HIGH, CRITICAL)); }
    public SeverityCounts plus(SeverityCounts other) {
        return new SeverityCounts(Math.addExact(LOW, other.LOW), Math.addExact(MEDIUM, other.MEDIUM),
                Math.addExact(HIGH, other.HIGH), Math.addExact(CRITICAL, other.CRITICAL));
    }
}
