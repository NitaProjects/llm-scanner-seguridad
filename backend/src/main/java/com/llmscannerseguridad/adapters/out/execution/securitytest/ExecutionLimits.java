package com.llmscannerseguridad.adapters.out.execution.securitytest;

public record ExecutionLimits(int maxTests, int requestBytes, int responseBytes, int textBytes,
        int evidenceBytes, int scanEvidenceBytes, long scanBudgetMs, int dnsMs, int connectMs, long intervalMs) {
    public ExecutionLimits {
        if (maxTests < 1 || maxTests > 10 || requestBytes < 1 || requestBytes > 65536
                || responseBytes < 1 || responseBytes > 1048576 || textBytes < 1 || textBytes > 262144
                || evidenceBytes < 1 || evidenceBytes > 524288 || scanEvidenceBytes < evidenceBytes
                || scanEvidenceBytes > 1048576 || scanBudgetMs < 60000 || scanBudgetMs > 600000
                || dnsMs < 1 || dnsMs > 2000 || connectMs < 1 || connectMs > 5000
                || intervalMs < 1000 || intervalMs > 600000) {
            throw new IllegalArgumentException("Los límites técnicos de ejecución no son válidos.");
        }
    }

    public static ExecutionLimits defaults() {
        return new ExecutionLimits(4, 65536, 262144, 65536, 131072, 524288, 300000, 2000, 5000, 1000);
    }
}
