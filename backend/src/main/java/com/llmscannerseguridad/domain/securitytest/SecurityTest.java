package com.llmscannerseguridad.domain.securitytest;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public record SecurityTest(UUID id, String code, int revision, String name,
        SecurityCategory category, String prompt) {
    public SecurityTest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(category);
        if (code == null || !code.matches("[A-Z0-9-]{1,64}") || revision < 1
                || name == null || name.isBlank() || name.codePointCount(0, name.length()) > 150
                || prompt == null || prompt.isBlank() || prompt.getBytes(StandardCharsets.UTF_8).length > 4096
                || invalidUnicode(prompt) || invalidUnicode(name) || prompt.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("La definición de prueba no es válida.");
        }
    }

    public static boolean invalidUnicode(String value) {
        return value.codePoints().anyMatch(cp -> cp >= 0xD800 && cp <= 0xDFFF);
    }

    @Override
    public String toString() {
        return "SecurityTest[id=" + id + ", code=" + code + ", revision=" + revision + "]";
    }
}
