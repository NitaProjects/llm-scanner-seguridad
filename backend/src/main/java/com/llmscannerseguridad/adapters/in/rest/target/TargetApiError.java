package com.llmscannerseguridad.adapters.in.rest.target;

import java.util.List;
import com.llmscannerseguridad.domain.target.TargetFieldViolation;

public record TargetApiError(String code, String message, List<TargetFieldViolation> fieldErrors) {
}
