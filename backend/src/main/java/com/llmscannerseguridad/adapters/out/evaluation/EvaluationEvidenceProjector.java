package com.llmscannerseguridad.adapters.out.evaluation;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public final class EvaluationEvidenceProjector implements EvaluationPreparationPort {
    public record Snapshot(EvaluationCriteria criteria, SyntheticContext context, String manifestHash,
            boolean exportAuthorized, String promptTemplateId, String promptHash, String schemaId,
            String schemaHash, String serializerVersion, int maxOutputTokens) { }
    private final ConcurrentHashMap<UUID, EvaluationScenarioPort.Entry> exportCorpus = new ConcurrentHashMap<>();
    private final EvaluatorSettings settings;
    private final EvaluatorMessages messages;
    private final Clock clock;
    public EvaluationEvidenceProjector(EvaluatorSettings settings, EvaluatorMessages messages, Clock clock) {
        this.settings = settings; this.messages = messages; this.clock = clock;
    }
    public EvaluationResult.Configuration configuration(UUID id, EvaluationCriteria criteria,
            EvaluationScenarioPort.Scenario scenario) {
        var entry = scenario == null ? null : scenario.entries().get(criteria.securityTestCode());
        var snapshot = new Snapshot(criteria, entry == null ? null : entry.context(),
                scenario == null ? null : scenario.manifestHash(), scenario != null && scenario.exportAuthorized(),
                "evaluator-prompt-v1", messages.promptHash(), "evaluator-output-v1", messages.schemaHash(), "serializer-v1", settings.maxOutputTokens());
        var serialized = EvaluationJson.canonical(snapshot);
        // Reserva adicional para señales/assessment y escapes de la proyección máxima.
        if (EvaluationJson.bytes(serialized) + settings.evidenceBytes() * 2 + 8192 > settings.recordBytes()) throw EvaluationJson.invalid();
        var configuration = new EvaluationResult.Configuration(criteria.criteriaId(), criteria.criteriaVersion(),
                scenario == null ? null : scenario.scenarioId(), scenario == null ? null : scenario.revision(),
                EvaluationResult.PROFILE, "hybrid-v1", "confidence-v1", "severity-v1", "evaluator-prompt-v1",
                "evaluator-output-v1", "HYBRID_WITH_SIGNALS", serialized, EvaluationJson.hash(serialized),
                settings.provider(), settings.model(), EvaluationJson.write(Map.of("max_output_tokens", settings.maxOutputTokens())));
        if (entry != null) exportCorpus.put(id, entry);
        return configuration;
    }
    public void release(UUID id) { exportCorpus.remove(id); }
    @Override public Prepared prepare(EvaluationResult pending, TestExecution execution, SecurityTest test) {
        var config = pending.configuration();
        if (config == null || config.configurationSnapshot() == null) throw EvaluationJson.invalid();
        if (!EvaluationJson.hash(EvaluationJson.canonical(EvaluationJson.parse(config.configurationSnapshot()))).equals(config.configurationHash())) {
            throw EvaluationJson.invalid();
        }
        var snapshot = EvaluationJson.MAPPER.readValue(config.configurationSnapshot(), Snapshot.class);
        if (snapshot.criteria() == null || !snapshot.criteria().securityTestCode().equals(test.code())
                || snapshot.criteria().securityTestRevision() != test.revision() || snapshot.criteria().category() != test.category()) {
            throw new EvaluationPreparationPort.Failure(EvaluationError.CRITERIA_UNAVAILABLE);
        }
        if (!snapshot.promptHash().equals(messages.promptHash()) || !snapshot.schemaHash().equals(messages.schemaHash())) throw EvaluationJson.invalid();
        var context = snapshot.context();
        var corpus = exportCorpus.get(pending.id());
        String response = execution.responseText();
        boolean complete = response != null;
        var shape = shape(response);
        var signals = new DeterministicRules().evaluate(snapshot.criteria(), context, test.prompt(),
                response == null ? "" : response, complete, shape, execution.responseRedacted());
        OutcomeReason reason = context == null ? OutcomeReason.CONTEXT_UNAVAILABLE : applicability(snapshot.criteria(), context);
        if (reason == null && !complete) reason = OutcomeReason.EVIDENCE_INCOMPLETE;
        if (reason == null && !snapshot.exportAuthorized()) reason = OutcomeReason.EXPORT_NOT_AUTHORIZED;
        if (reason == null && (corpus == null || !corpus.exportableResponses().contains(response))) reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
        int evidenceBytes = EvaluationJson.bytes(response);
        if (reason == null && evidenceBytes > settings.evidenceBytes()) { reason = OutcomeReason.EVIDENCE_TOO_LARGE; complete = false; }
        boolean strong = signals.stream().anyMatch(s -> s.strength() == RuleSignal.Strength.STRONG_POSITIVE);
        if (reason == null && execution.responseRedacted() && !strong && (response.contains("[REDACTED_CREDENTIAL]")
                || signals.stream().anyMatch(signal -> signal.reasonCode().equals("EVIDENCE_REDACTED")))) {
            reason = OutcomeReason.EVIDENCE_INCOMPLETE; complete = false;
        }
        EvaluatorInput input = reason == null ? new EvaluatorInput(snapshot.criteria(), test.prompt(), context,
                signals, response, complete, execution.responseRedacted()) : null;
        String serialized = input == null ? null : messages.request(input, settings);
        int inputBytes = EvaluationJson.bytes(serialized);
        if (reason == null && inputBytes > settings.inputBytes()) { reason = OutcomeReason.EVIDENCE_TOO_LARGE; complete = false; input = null; }
        String projection = input == null ? null : EvaluationJson.canonical(input);
        if (EvaluationJson.bytes(projection) + EvaluationJson.bytes(config.configurationSnapshot())
                + EvaluationJson.bytes(EvaluationJson.write(signals)) + 8192 > settings.recordBytes()) throw EvaluationJson.invalid();
        var evidence = new EvaluationResult.Evidence(projection, complete, execution.responseRedacted(), inputBytes,
                evidenceBytes, projection == null ? null : EvaluationJson.hash(projection));
        var component = reason == null ? EvaluationResult.LlmComponent.pending()
                : new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null);
        var running = pending.start(clock.instant(), execution.finishedAt(), signals, evidence, component);
        return new Prepared(running, snapshot.criteria(), context, input, reason);
    }
    private OutcomeReason applicability(EvaluationCriteria criteria, SyntheticContext context) {
        for (var key : criteria.requiredContextKeys()) {
            Object value = switch (key) {
                case "restriction" -> context.restriction(); case "reservedValue" -> context.reservedValue();
                case "hiddenContext" -> context.hiddenContext(); case "outputContract" -> context.outputContract() != null && context.outputContract().securityRelevant() ? context.outputContract() : null;
                default -> null;
            };
            if (value == null || value instanceof String text && text.isBlank()) return OutcomeReason.CRITERION_NOT_APPLICABLE;
        }
        return null;
    }
    private DeterministicRules.OutputShape shape(String response) {
        try {
            var value = EvaluationJson.parse(response);
            if (value instanceof Map<?, ?> map) {
                var types = new java.util.HashMap<String,SyntheticContext.ValueType>();
                map.forEach((key,item) -> types.put((String)key, item == null ? SyntheticContext.ValueType.NULL
                        : item instanceof String ? SyntheticContext.ValueType.STRING : item instanceof Number ? SyntheticContext.ValueType.NUMBER
                        : item instanceof Boolean ? SyntheticContext.ValueType.BOOLEAN : item instanceof List<?> ? SyntheticContext.ValueType.ARRAY : SyntheticContext.ValueType.OBJECT));
                return new DeterministicRules.OutputShape(true,types.keySet(),types);
            }
        } catch (RuntimeException ignored) { }
        return new DeterministicRules.OutputShape(false, Set.of(), Map.of());
    }
}
