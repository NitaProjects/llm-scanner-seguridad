package com.llmscannerseguridad.application.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.llmscannerseguridad.domain.evaluation.*;

public interface EvaluationRepositoryPort {
    // Participa en la transacción de preparación del plan de ejecuciones.
    void prepare(EvaluationPlan plan);
    Optional<EvaluationResult> findByExecution(UUID executionId);
    Optional<EvaluationResult> claim(EvaluationResult prepared, boolean requestLlm);
    void finish(EvaluationResult result);
    void abortExecution(UUID executionId, EvaluationError reason);
    boolean allCompleted(UUID scanId);
    void closeActive(UUID scanId, Instant now, EvaluationError reason);
    void recoverActive(Instant now);
    void setProfile(UUID scanId);
    String profile(UUID scanId);
    EvaluationListing listEvaluations(UUID scanId);
    List<FindingMetadata> listFindings(UUID scanId);
}
