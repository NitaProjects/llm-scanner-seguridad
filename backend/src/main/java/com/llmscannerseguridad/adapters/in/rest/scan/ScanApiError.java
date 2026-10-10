package com.llmscannerseguridad.adapters.in.rest.scan;

import java.util.List;

public record ScanApiError(String code, String message, List<Object> fieldErrors) {
}
