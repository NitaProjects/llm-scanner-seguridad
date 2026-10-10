package com.llmscannerseguridad.adapters.out.evaluation;

import java.math.BigDecimal;

public record EvaluatorSettings(String provider, String model, int evidenceBytes, int inputBytes,
        int responseBytes, int maxOutputTokens, int timeoutMs, int maxCalls, int intervalMs,
        int recordBytes, BigDecimal maxEstimatedCostUsd, BigDecimal estimatedMaxCallCostUsd,
        String pricingProvider, String pricingModel, String pricingVersion, String estimateVersion,
        String pricingDate, String currency, BigDecimal inputPricePerMillion, BigDecimal outputPricePerMillion) {
    public EvaluatorSettings {
        if (!"OPENAI".equalsIgnoreCase(provider) || model == null || !model.matches("[A-Za-z0-9._:-]{1,128}")
                || evidenceBytes < 1024 || evidenceBytes > 16384 || inputBytes < 8192 || inputBytes > 24576
                || responseBytes < 4096 || responseBytes > 16384 || maxOutputTokens < 256 || maxOutputTokens > 1024
                || timeoutMs < 1000 || timeoutMs > 20000 || maxCalls < 1 || maxCalls > 4
                || intervalMs < 1000 || intervalMs > 60000 || recordBytes < 16384 || recordBytes > 65536
                || maxEstimatedCostUsd != null && maxEstimatedCostUsd.signum() <= 0) throw EvaluationJson.invalid();
        provider = "OPENAI";
    }
    public boolean pricingValid() {
        return tariffValid() && estimatedMaxCallCostUsd != null && estimatedMaxCallCostUsd.signum() > 0;
    }
    public boolean tariffValid() {
        return provider.equalsIgnoreCase(pricingProvider) && model.equals(pricingModel)
                && pricingVersion != null && pricingVersion.matches("[A-Za-z0-9.-]{1,64}")
                && estimateVersion != null && estimateVersion.matches("[A-Za-z0-9.-]{1,64}")
                && pricingDate != null && validDate(pricingDate) && "USD".equals(currency)
                && inputPricePerMillion != null && inputPricePerMillion.signum() >= 0
                && outputPricePerMillion != null && outputPricePerMillion.signum() >= 0;
    }
    private static boolean validDate(String date) {
        try { java.time.LocalDate.parse(date); return true; } catch (Exception error) { return false; }
    }
}
