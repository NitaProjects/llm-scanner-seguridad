package com.llmscannerseguridad.domain.target;

import java.util.List;

public final class TargetValidationException extends RuntimeException {
    private final List<TargetFieldViolation> fieldErrors;

    public TargetValidationException(List<TargetFieldViolation> fieldErrors) {
        super("Los datos del target no son válidos.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<TargetFieldViolation> fieldErrors() {
        return fieldErrors;
    }

    public static TargetValidationException field(String field, String code) {
        return new TargetValidationException(List.of(new TargetFieldViolation(field, code,
                "El campo no cumple las reglas de configuración.")));
    }
}
