package com.llmscannerseguridad.application.evaluation;

import com.llmscannerseguridad.domain.evaluation.EvaluationCriteria;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public interface EvaluationCriteriaCatalogPort {
    EvaluationCriteria find(String code, int revision, com.llmscannerseguridad.domain.securitytest.SecurityCategory category);

    default EvaluationCriteria find(SecurityTest test) { return find(test.code(), test.revision(), test.category()); }
}
