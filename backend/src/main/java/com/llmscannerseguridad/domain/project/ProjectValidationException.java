package com.llmscannerseguridad.domain.project;

import java.util.List;

public class ProjectValidationException extends RuntimeException {

    private final List<ProjectFieldViolation> fieldErrors;

    public ProjectValidationException(List<ProjectFieldViolation> fieldErrors) {
        super("Los datos del proyecto no son válidos.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ProjectFieldViolation> fieldErrors() {
        return fieldErrors;
    }
}
