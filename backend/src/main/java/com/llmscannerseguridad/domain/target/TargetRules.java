package com.llmscannerseguridad.domain.target;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class TargetRules {
    private static final Pattern HEADER = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}");
    private static final Set<String> RESERVED = Set.of("authorization", "proxy-authorization", "cookie",
            "set-cookie", "host", "content-length", "content-type", "connection", "transfer-encoding",
            "te", "trailer", "upgrade", "keep-alive", "proxy-connection", "forwarded");

    private TargetRules() {
    }

    public static void noNullCharacter(String text, String field) {
        if (text != null && text.indexOf('\0') >= 0) {
            throw TargetValidationException.field(field, "INVALID_CHARACTER");
        }
    }

    public static String trim(String text) {
        if (text == null) {
            return null;
        }
        int start = 0;
        int end = text.length();
        while (start < end && whitespace(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        while (end > start && whitespace(text.codePointBefore(end))) {
            end -= Character.charCount(text.codePointBefore(end));
        }
        return text.substring(start, end);
    }

    public static boolean whitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }

    public static int bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    public static void length(String text, int maximum, String field) {
        if (text.codePointCount(0, text.length()) > maximum) {
            throw TargetValidationException.field(field, "TOO_LONG");
        }
    }

    public static boolean controls(String text) {
        return text.codePoints().anyMatch(Character::isISOControl);
    }

    public static void headerName(String name, String field, boolean authentication) {
        noNullCharacter(name, field);
        if (name != null && name.length() > 128) {
            throw TargetValidationException.field(field, "TOO_LONG");
        }
        if (name == null || !HEADER.matcher(name).matches()) {
            throw TargetValidationException.field(field, "INVALID_FORMAT");
        }
        String lower = name.toLowerCase(Locale.ROOT);
        if (RESERVED.contains(lower) || lower.startsWith("x-forwarded-")
                || (!authentication && Set.of("x-api-key", "api-key").contains(lower))) {
            throw TargetValidationException.field(field, "NOT_ALLOWED");
        }
    }

    public static void headers(Map<String, String> headers, String authHeaderName) {
        if (headers.size() > 20) {
            throw TargetValidationException.field("headers", "TOO_LONG");
        }
        var names = new HashSet<String>();
        int total = 0;
        for (var entry : headers.entrySet()) {
            headerName(entry.getKey(), "headers", false);
            if (!names.add(entry.getKey().toLowerCase(Locale.ROOT))
                    || entry.getKey().equalsIgnoreCase(authHeaderName)) {
                throw TargetValidationException.field("headers", "CONFLICT");
            }
            String value = entry.getValue();
            if (value == null) {
                throw TargetValidationException.field("headers", "INVALID_TYPE");
            }
            if (value.codePoints().anyMatch(cp -> cp <= 31 || cp == 127)) {
                throw TargetValidationException.field("headers", "INVALID_CHARACTER");
            }
            if (bytes(value) > 4096) {
                throw TargetValidationException.field("headers", "TOO_LONG");
            }
            total += bytes(entry.getKey()) + bytes(value);
        }
        if (total > 16384) {
            throw TargetValidationException.field("headers", "TOO_LONG");
        }
    }

    public static void secret(String secret, AuthType type) {
        if (secret == null || trim(secret).isEmpty()) {
            throw TargetValidationException.field("authConfig.secretValue", "REQUIRED");
        }
        if (controls(secret) || secret.codePoints().anyMatch(cp ->
                cp >= Character.MIN_SURROGATE && cp <= Character.MAX_SURROGATE)) {
            throw TargetValidationException.field("authConfig.secretValue", "INVALID_CHARACTER");
        }
        if (bytes(secret) > 8192) {
            throw TargetValidationException.field("authConfig.secretValue", "TOO_LONG");
        }
        if (type == AuthType.BEARER_TOKEN && secret.codePoints().anyMatch(TargetRules::whitespace)) {
            throw TargetValidationException.field("authConfig.secretValue", "INVALID_FORMAT");
        }
    }
}
