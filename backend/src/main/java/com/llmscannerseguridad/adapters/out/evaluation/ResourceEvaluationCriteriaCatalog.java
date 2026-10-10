package com.llmscannerseguridad.adapters.out.evaluation;

import java.util.List;
import com.llmscannerseguridad.application.evaluation.EvaluationCriteriaCatalogPort;
import com.llmscannerseguridad.domain.evaluation.EvaluationCriteria;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public final class ResourceEvaluationCriteriaCatalog implements EvaluationCriteriaCatalogPort {
    private final List<EvaluationCriteria> criteria;
    public ResourceEvaluationCriteriaCatalog() {
        var text = EvaluationJson.resource("criteria-v1.json", 131072);
        EvaluationJson.parse(text);
        criteria = List.of(EvaluationJson.MAPPER.readValue(text, EvaluationCriteria[].class));
        if (criteria.size() != 4 || criteria.stream().map(EvaluationCriteria::category).distinct().count() != 4) {
            throw EvaluationJson.invalid();
        }
    }
    @Override public EvaluationCriteria find(String code, int revision, com.llmscannerseguridad.domain.securitytest.SecurityCategory category) {
        return criteria.stream().filter(c -> c.securityTestCode().equals(code)
                && c.securityTestRevision() == revision && c.category() == category).findFirst()
                .orElseThrow(() -> new IllegalStateException("CRITERIA_UNAVAILABLE"));
    }
}
