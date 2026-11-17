package com.llmscannerseguridad.application.evaluation;

import java.util.Set;
import java.util.UUID;

public interface EphemeralEvaluationCorpus {
    Set<UUID> retainedIds();
    void release(UUID id);
}
