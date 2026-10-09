package com.llmscannerseguridad.domain.project;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public record Project(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {

    public Project {
        Objects.requireNonNull(id);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("La fecha de actualización no puede preceder a la creación.");
        }

        name = trim(name);
        description = trim(description);
        validateCreation(name, description);
        if (description != null && description.isEmpty()) {
            description = null;
        }
    }

    public static void validateCreation(String name, String description) {
        name = trim(name);
        description = trim(description);
        var errors = new ArrayList<ProjectFieldViolation>();
        if (name == null || name.isEmpty()) {
            errors.add(new ProjectFieldViolation("name", "REQUIRED", "El nombre es obligatorio."));
        } else if (name.codePointCount(0, name.length()) > 120) {
            errors.add(new ProjectFieldViolation("name", "TOO_LONG", "El nombre admite hasta 120 caracteres."));
        }
        if (description != null && description.codePointCount(0, description.length()) > 2000) {
            errors.add(new ProjectFieldViolation("description", "TOO_LONG", "La descripción admite hasta 2000 caracteres."));
        }
        if (name != null && name.indexOf('\0') >= 0) {
            errors.add(new ProjectFieldViolation("name", "INVALID_CHARACTER", "El nombre no admite el carácter U+0000."));
        }
        if (description != null && description.indexOf('\0') >= 0) {
            errors.add(new ProjectFieldViolation("description", "INVALID_CHARACTER", "La descripción no admite el carácter U+0000."));
        }
        if (!errors.isEmpty()) {
            throw new ProjectValidationException(errors);
        }
    }

    private static String trim(String text) {
        if (text == null) {
            return null;
        }
        int start = 0;
        int end = text.length();
        while (start < end && isWhitespace(text.codePointAt(start))) {
            start += Character.charCount(text.codePointAt(start));
        }
        while (end > start && isWhitespace(text.codePointBefore(end))) {
            end -= Character.charCount(text.codePointBefore(end));
        }
        return text.substring(start, end);
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
