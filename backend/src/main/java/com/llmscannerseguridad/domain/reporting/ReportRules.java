package com.llmscannerseguridad.domain.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;

public final class ReportRules {
    public static final String VERSION = "report-v1";
    public static final String LOCALE = "es";
    public static final List<SecurityCategory> CATEGORIES = List.of(SecurityCategory.LLM01, SecurityCategory.LLM02,
            SecurityCategory.LLM08, SecurityCategory.LLM10);
    public static final List<String> TEST_CODES = List.of("LLM01-INSTRUCTION-001", "LLM02-CANARY-001",
            "LLM08-CONTEXT-001", "LLM10-MARKUP-001");
    private ReportRules() { }
    public static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Informe de seguridad no válido.");
    }
    public static void time(Instant value) {
        require(value != null && value.getNano() % 1000 == 0);
        int year = value.atOffset(java.time.ZoneOffset.UTC).getYear();
        require(year >= 1 && year <= 9999);
    }
    public static void text(String value, int maximum) {
        require(value != null && !value.isBlank() && value.codePointCount(0, value.length()) <= maximum);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            require(c != 0 && c != '<' && c != '>' && c != '`');
            if (Character.isHighSurrogate(c)) require(++i < value.length() && Character.isLowSurrogate(value.charAt(i)));
            else require(!Character.isLowSurrogate(c));
        }
    }
    public static void nullableText(String value, int maximum) { if (value != null) text(value, maximum); }
    public static void confidence(BigDecimal value) {
        require(value != null && value.signum() >= 0 && value.compareTo(BigDecimal.ONE) <= 0);
    }
    public static void test(String code, int revision, SecurityCategory category) {
        require(category != null && revision == 1 && TEST_CODES.get(CATEGORIES.indexOf(category)).equals(code));
    }
}
