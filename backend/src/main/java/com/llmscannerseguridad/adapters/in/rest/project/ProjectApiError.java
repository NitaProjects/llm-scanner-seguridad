package com.llmscannerseguridad.adapters.in.rest.project;

import java.util.List;

public record ProjectApiError(String code, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String code, String message) {
    }
}
