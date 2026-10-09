package com.llmscannerseguridad.domain.target;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TargetConfigurationTest {
    private TargetSettings settings(String name, Map<String, String> headers, AuthType type, String header, int timeout) {
        return new TargetSettings(name, new TargetUrl("https://example.test/chat"),
                "{\"message\":\"{{PROMPT}}\"}", "$.text", headers, type, header, timeout);
    }

    @Test
    void unicodeTrimOptionalNameDatesAndOpaqueCredential() {
        String symbol = "\uD83D\uDD12";
        var value = settings(" \u00a0" + symbol.repeat(120) + "\u00a0 ", Map.of(), AuthType.NONE, null, 1000);
        assertThat(value.name()).isEqualTo(symbol.repeat(120));
        assertThat(settings(" ", Map.of(), AuthType.NONE, null, 60000).name()).isNull();
        assertThatThrownBy(() -> settings(symbol.repeat(121), Map.of(), AuthType.NONE, null, 10000))
                .isInstanceOf(TargetValidationException.class);
        for (int timeout : new int[] {999, 60001}) {
            assertThatThrownBy(() -> settings(null, Map.of(), AuthType.NONE, null, timeout))
                    .isInstanceOf(TargetValidationException.class);
        }
        assertThatThrownBy(() -> settings("\0 nombre", Map.of(), AuthType.NONE, null, 10000))
                .isInstanceOfSatisfying(TargetValidationException.class, error ->
                        assertThat(error.fieldErrors().getFirst().code()).isEqualTo("INVALID_CHARACTER"));
        Instant now = Instant.parse("2026-10-10T10:00:00.123456Z");
        var target = new TargetConfiguration(UUID.randomUUID(), UUID.randomUUID(), value, null, now, now);
        assertThat(target.updatedAt()).isEqualTo(target.createdAt());
        assertThatThrownBy(() -> new TargetConfiguration(target.id(), target.projectId(), value, null,
                now, now.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        byte[] input = {1, 2, 3};
        var encrypted = new EncryptedCredential(input);
        input[0] = 9;
        byte[] returned = encrypted.bytes();
        returned[1] = 9;
        assertThat(encrypted.bytes()).containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(encrypted.toString()).doesNotContain("1, 2, 3");
    }

    @Test
    void headerNamesReservedCaseCollisionsControlsAndUtf8Limits() {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("X-Correlation-Id", "demo");
        settings(null, headers, AuthType.NONE, null, 10000);
        for (String name : new String[] {"Authorization", "Proxy-Authorization", "Cookie", "Set-Cookie",
                "X-API-Key", "API-Key", "Host", "Content-Length", "Content-Type", "Connection",
                "Transfer-Encoding", "TE", "Trailer", "Upgrade", "Keep-Alive", "Proxy-Connection",
                "Forwarded", "X-Forwarded-For", "bad name", "ñ", "X".repeat(129)}) {
            assertThatThrownBy(() -> settings(null, Map.of(name, "sintetico"), AuthType.NONE, null, 10000))
                    .isInstanceOf(TargetValidationException.class);
        }
        assertThatThrownBy(() -> settings(null, Map.of("Accept", "a", "accept", "b"), AuthType.NONE, null, 10000))
                .isInstanceOf(TargetValidationException.class);
        assertThatThrownBy(() -> settings(null, Map.of("X-Custom", "a"), AuthType.API_KEY_HEADER, "x-custom", 10000))
                .isInstanceOf(TargetValidationException.class);
        for (String value : new String[] {"a\rb", "a\nb", "a\0b", "a\u007fb", "é".repeat(2049)}) {
            assertThatThrownBy(() -> settings(null, Map.of("X", value), AuthType.NONE, null, 10000))
                    .isInstanceOf(TargetValidationException.class);
        }
        settings(null, Map.of("X", "é".repeat(2048)), AuthType.NONE, null, 10000);
        settings(null, Map.of("X".repeat(128), ""), AuthType.NONE, null, 10000);
        var maximum = new LinkedHashMap<String, String>();
        for (int i = 0; i < 20; i++) {
            maximum.put("H" + i, "");
        }
        settings(null, maximum, AuthType.NONE, null, 10000);
        maximum.put("Extra", "");
        assertThatThrownBy(() -> settings(null, maximum, AuthType.NONE, null, 10000))
                .isInstanceOf(TargetValidationException.class);
        var total = Map.of("A", "x".repeat(4095), "B", "x".repeat(4095),
                "C", "x".repeat(4095), "D", "x".repeat(4095));
        settings(null, total, AuthType.NONE, null, 10000);
        var over = new LinkedHashMap<>(total);
        over.put("E", "");
        assertThatThrownBy(() -> settings(null, over, AuthType.NONE, null, 10000))
                .isInstanceOf(TargetValidationException.class);
        settings(null, Map.of(), AuthType.API_KEY_HEADER, "X-API-Key", 10000);
        settings(null, Map.of(), AuthType.API_KEY_HEADER, "API-Key", 10000);
        assertThatThrownBy(() -> settings(null, Map.of(), AuthType.API_KEY_HEADER, "Host", 10000))
                .isInstanceOf(TargetValidationException.class);
    }

    @Test
    void secretsRejectIsolatedSurrogatesAndAcceptValidSupplementaryPairs() {
        for (AuthType type : new AuthType[] {AuthType.BEARER_TOKEN, AuthType.API_KEY_HEADER}) {
            for (String value : new String[] {"A\uD800B", "A\uDC00B", "\uD800", "\uDC00"}) {
                assertThatThrownBy(() -> TargetRules.secret(value, type))
                        .isInstanceOfSatisfying(TargetValidationException.class, error ->
                                assertThat(error.fieldErrors()).containsExactly(
                                        new TargetFieldViolation("authConfig.secretValue", "INVALID_CHARACTER",
                                                "El campo no cumple las reglas de configuración.")));
            }
            TargetRules.secret("A\uD83D\uDD12B", type);
        }
    }

    @Test
    void secretsAreNotTrimmedAndHaveAuthenticationSpecificLimits() {
        TargetRules.secret(" sintético ", AuthType.API_KEY_HEADER);
        TargetRules.secret("é".repeat(4096), AuthType.API_KEY_HEADER);
        for (String value : new String[] {"", " ", "\0synthetic", "a\rb", "a\u007fb", "a\u0085b", "é".repeat(4097)}) {
            assertThatThrownBy(() -> TargetRules.secret(value, AuthType.API_KEY_HEADER))
                    .isInstanceOf(TargetValidationException.class);
        }
        assertThatThrownBy(() -> TargetRules.secret(" synthetic ", AuthType.BEARER_TOKEN))
                .isInstanceOf(TargetValidationException.class);
        assertThatThrownBy(() -> TargetRules.secret(null, AuthType.API_KEY_HEADER))
                .isInstanceOf(TargetValidationException.class);
    }
}
