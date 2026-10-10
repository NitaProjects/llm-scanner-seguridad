package com.llmscannerseguridad.application.evaluation;

import java.util.List;

public record EvaluationListing(String evaluationProfile, List<EvaluationMetadata> items) {
    public EvaluationListing { items = List.copyOf(items); }
}
