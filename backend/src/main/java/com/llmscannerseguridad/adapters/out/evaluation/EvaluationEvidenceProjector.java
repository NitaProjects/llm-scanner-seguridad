package com.llmscannerseguridad.adapters.out.evaluation;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.TestExecution;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import com.llmscannerseguridad.adapters.out.execution.securitytest.ExecutionLimits;

public final class EvaluationEvidenceProjector implements EvaluationPreparationPort, EphemeralEvaluationCorpus {
    private static final String INERT_SPAN = "<span data-scanner=\"demo\">SCANNER_OUTPUT_DEMO</span>";
    // Limita el trabajo adicional por rama. El parser acota aparte la profundidad de objetos y listas.
    private static final int MAX_EMBEDDED_JSON_LAYERS = 4;
    public record Snapshot(EvaluationCriteria criteria, SyntheticContext context, String manifestHash,
            boolean exportAuthorized, String promptTemplateId, String promptHash, String schemaId,
            String schemaHash, String serializerVersion, int maxOutputTokens) { }
    public record BoundSnapshot(String policy, EvaluationScenarioPort.Binding binding, Snapshot snapshot) { }
    private final ConcurrentHashMap<UUID, EvaluationScenarioPort.Entry> exportCorpus = new ConcurrentHashMap<>();
    private final EvaluatorSettings settings;
    private final EvaluatorMessages messages;
    private final Clock clock;
    private final int textBytes;
    public EvaluationEvidenceProjector(EvaluatorSettings settings, EvaluatorMessages messages, Clock clock) {
        this(settings, messages, clock, ExecutionLimits.defaults());
    }
    public EvaluationEvidenceProjector(EvaluatorSettings settings, EvaluatorMessages messages, Clock clock, ExecutionLimits limits) {
        this.settings = settings; this.messages = messages; this.clock = clock; this.textBytes = limits.textBytes();
    }
    public EvaluationResult.Configuration configuration(UUID id, EvaluationCriteria criteria,
            EvaluationScenarioPort.Scenario scenario) {
        var entry = scenario == null ? null : scenario.entries().get(criteria.securityTestCode());
        var snapshot = new Snapshot(criteria, entry == null ? null : entry.context(),
                scenario == null ? null : scenario.manifestHash(), scenario != null && scenario.exportAuthorized(),
                "evaluator-prompt-v1", messages.promptHash(), "evaluator-output-v1", messages.schemaHash(), "serializer-v1", settings.maxOutputTokens());
        boolean v2 = scenario != null && scenario.v2();
        String policy = v2 ? "hybrid-v2" : "hybrid-v1";
        var serialized = EvaluationJson.canonical(v2 ? new BoundSnapshot(policy, scenario.binding(), snapshot) : snapshot);
        // V1 conserva la reserva de proyección máxima. V2 comprueba la proyección solo si la necesita.
        if (EvaluationJson.bytes(serialized) + (v2 ? 0 : settings.evidenceBytes() * 2 + 8192) > settings.recordBytes()) throw EvaluationJson.invalid();
        if (com.llmscannerseguridad.privacy.ProtectionScope.active()) {
            com.llmscannerseguridad.privacy.DurableEvidenceProtection.requireDocument(serialized, settings.recordBytes());
        }
        var configuration = new EvaluationResult.Configuration(criteria.criteriaId(), criteria.criteriaVersion(),
                scenario == null ? null : scenario.scenarioId(), scenario == null ? null : scenario.revision(),
                EvaluationResult.PROFILE, policy, "confidence-v1", "severity-v1", "evaluator-prompt-v1",
                "evaluator-output-v1", "HYBRID_WITH_SIGNALS", serialized, EvaluationJson.hash(serialized),
                settings.provider(), settings.model(), EvaluationJson.write(Map.of("max_output_tokens", settings.maxOutputTokens())));
        if (entry != null) exportCorpus.put(id, entry);
        return configuration;
    }
    @Override public Set<UUID> retainedIds() { return Set.copyOf(exportCorpus.keySet()); }
    @Override public void release(UUID id) { exportCorpus.remove(id); }

    @Override public Prepared prepare(EvaluationResult pending, TestExecution execution, SecurityTest test,
            StructuredInteractionEvidence.Context context) {
        var historical = prepare(pending, execution, test);
        if (!historical.requestLlm()) return historical;
        var trace = context.evidence();
        var input = historical.input();
        var additional = new ArrayList<StructuredInteractionEvidence.Observation>();
        var blocked = new HashSet<>(trace.localCorrelationIds());
        OutcomeReason reason = null;
        for (var source : StructuredInteractionEvidence.Source.values()) {
            if (source == StructuredInteractionEvidence.Source.USER_INPUT) continue;
            var records = trace.observations().stream().filter(item -> item.source() == source).toList();
            var availability = trace.availability().getOrDefault(source, StructuredInteractionEvidence.Availability.INCOMPLETE);
            boolean declared = trace.semanticSources().containsKey(source);
            var allowed = declared && availability == StructuredInteractionEvidence.Availability.COMPLETE
                    ? records.stream().filter(item -> item.exportable() && item.complete()).toList() : List.<StructuredInteractionEvidence.Observation>of();
            if (allowed.size() == records.size() && !allowed.isEmpty()) additional.addAll(allowed);
            else {
                records.forEach(item -> {
                    if (item.value() != null && !item.value().isEmpty()) blocked.add(item.value());
                    if (item.alias() != null && !item.alias().isEmpty()) blocked.add(item.alias());
                });
                if (reason == null && Boolean.TRUE.equals(trace.semanticSources().get(source))) {
                    reason = switch (availability) {
                        case TOO_LARGE -> OutcomeReason.EVIDENCE_TOO_LARGE;
                        case INCOMPLETE -> OutcomeReason.EVIDENCE_INCOMPLETE;
                        case COMPLETE -> OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
                    };
                }
            }
        }
        if (additional.stream().anyMatch(item -> item.relatedOrdinal() != null
                && additional.stream().noneMatch(call -> call.kind() == StructuredInteractionEvidence.Kind.REQUEST
                    && call.ordinal() == item.relatedOrdinal()))) {
            if (Boolean.TRUE.equals(trace.semanticSources().get(StructuredInteractionEvidence.Source.TOOL_RESULT)) && reason == null) {
                reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
            }
            additional.removeIf(item -> {
                if (item.source() != StructuredInteractionEvidence.Source.TOOL_RESULT) return false;
                blocked.add(item.value());
                return true;
            });
        }
        var candidate = input.withAdditionalSources(additional);
        String projection = messages.projection(candidate);
        Object candidateData = EvaluationJson.parse(projection);
        if (reason == null && (trace.inspectionIncomplete() || containsBlocked(candidateData, blocked)
                || blocksSpanExport(candidateData, MAX_EMBEDDED_JSON_LAYERS))) {
            reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
        }
        if (reason == null && containsBlocked(messages.inspectionView(candidate, settings), blocked)) reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
        int inputBytes = reason == null ? EvaluationJson.bytes(messages.request(candidate, settings)) : 0;
        if (reason == null && !additional.isEmpty() && (inputBytes > settings.inputBytes()
                || EvaluationJson.bytes(projection) + EvaluationJson.bytes(pending.configuration().configurationSnapshot())
                    + EvaluationJson.bytes(EvaluationJson.write(historical.result().deterministicSignals())) + 8192 > settings.recordBytes()
                || EvaluationJson.bytes(EvaluationJson.write(additional)) + historical.result().evidence().evidenceBytes()
                    > Math.min(settings.evidenceBytes(), textBytes))) reason = OutcomeReason.EVIDENCE_TOO_LARGE;
        if (reason == null && additional.isEmpty()) return historical;
        var previous = historical.result().evidence();
        var evidence = new EvaluationResult.Evidence(null, previous.complete(), previous.redacted(),
                reason == null ? inputBytes : 0, previous.evidenceBytes(), null);
        var component = reason == null ? EvaluationResult.LlmComponent.pending()
                : new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null);
        var signals = historical.result().deterministicSignals();
        boolean localDecision = matched(signals, RuleSignal.Strength.STRONG_POSITIVE)
                && !matched(signals, RuleSignal.Strength.STRONG_NEGATIVE);
        return new Prepared(pending.start(clock.instant(), execution.finishedAt(), signals, evidence, component),
                historical.criteria(), historical.context(), reason == null ? candidate : null, localDecision ? null : reason);
    }

    private static boolean containsBlocked(Object node, Set<String> blocked) {
        var protectedValues = new HashSet<>(blocked);
        protectedValues.addAll(com.llmscannerseguridad.privacy.ProtectionScope.values());
        return !com.llmscannerseguridad.privacy.ProtectionScope.complete()
                || new com.llmscannerseguridad.privacy.ProtectedContentInspector().inspectDocument(
                EvaluationJson.write(node), protectedValues, 65536).state()
                != com.llmscannerseguridad.privacy.ProtectedContentInspector.State.SAFE;
    }

    @Override public Prepared prepare(EvaluationResult pending, TestExecution execution, SecurityTest test) {
        var config = pending.configuration();
        if (config == null || config.configurationSnapshot() == null) throw EvaluationJson.invalid();
        if (!EvaluationJson.hash(EvaluationJson.canonical(EvaluationJson.parse(config.configurationSnapshot()))).equals(config.configurationHash())) {
            throw EvaluationJson.invalid();
        }
        boolean v2 = "hybrid-v2".equals(config.policyVersion());
        Snapshot snapshot;
        if (v2) {
            var bound = EvaluationJson.MAPPER.readValue(config.configurationSnapshot(), BoundSnapshot.class);
            if (!config.policyVersion().equals(bound.policy()) || bound.binding() == null
                    || !execution.targetConfigurationId().equals(bound.binding().targetConfigurationId())
                    || !execution.targetUpdatedAt().equals(bound.binding().targetUpdatedAt())
                    || !execution.id().equals(pending.testExecutionId()) || !execution.scanId().equals(pending.scanId())
                    || !execution.securityTestId().equals(test.id()) || !pending.securityTestId().equals(test.id())
                    || execution.status() != com.llmscannerseguridad.domain.execution.ExecutionStatus.COMPLETED) throw EvaluationJson.invalid();
            snapshot = bound.snapshot();
        } else {
            snapshot = EvaluationJson.MAPPER.readValue(config.configurationSnapshot(), Snapshot.class);
        }
        if (snapshot.criteria() == null || !snapshot.criteria().securityTestCode().equals(test.code())
                || snapshot.criteria().securityTestRevision() != test.revision() || snapshot.criteria().category() != test.category()) {
            throw new EvaluationPreparationPort.Failure(EvaluationError.CRITERIA_UNAVAILABLE);
        }
        if (!snapshot.promptHash().equals(messages.promptHash()) || !snapshot.schemaHash().equals(messages.schemaHash())) throw EvaluationJson.invalid();
        if (v2) return prepareV2(pending, execution, test, snapshot);
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
        if (serialized != null && com.llmscannerseguridad.privacy.ProtectionScope.active() && containsBlocked(messages.inspectionView(input, settings), Set.of())) {
            reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE; input = null; serialized = null;
        }
        int inputBytes = EvaluationJson.bytes(serialized);
        if (reason == null && inputBytes > settings.inputBytes()) { reason = OutcomeReason.EVIDENCE_TOO_LARGE; complete = false; input = null; }
        String projection = input == null ? null : messages.projection(input);
        if (EvaluationJson.bytes(projection) + EvaluationJson.bytes(config.configurationSnapshot())
                + EvaluationJson.bytes(EvaluationJson.write(signals)) + 8192 > settings.recordBytes()) throw EvaluationJson.invalid();
        var evidence = new EvaluationResult.Evidence(projection, complete, execution.responseRedacted(), inputBytes,
                evidenceBytes, projection == null ? null : EvaluationJson.hash(projection));
        var component = reason == null ? EvaluationResult.LlmComponent.pending()
                : new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null);
        var running = pending.start(clock.instant(), execution.finishedAt(), signals, evidence, component);
        return new Prepared(running, snapshot.criteria(), context, input, reason);
    }

    private Prepared prepareV2(EvaluationResult pending, TestExecution execution, SecurityTest test, Snapshot snapshot) {
        var criteria = snapshot.criteria();
        var context = snapshot.context();
        String response = execution.responseText();
        int bytes = EvaluationJson.bytes(response);
        boolean complete = response != null && bytes <= textBytes;
        boolean redacted = execution.responseRedacted();
        OutcomeReason reason = bytes > textBytes ? OutcomeReason.EVIDENCE_TOO_LARGE
                : context == null ? OutcomeReason.CONTEXT_UNAVAILABLE : applicability(criteria, context);
        if (reason == null && !complete) reason = OutcomeReason.EVIDENCE_INCOMPLETE;
        var signals = new DeterministicRules().evaluate(criteria, context, test.prompt(),
                complete ? response : "", complete && reason == null,
                complete && reason == null && !redacted && criteria.category().name().equals("LLM10") ? shape(response) : null,
                redacted, true);
        boolean strong = matched(signals, RuleSignal.Strength.STRONG_POSITIVE);
        boolean safe = matched(signals, RuleSignal.Strength.STRONG_NEGATIVE);
        boolean support = matched(signals, RuleSignal.Strength.SUPPORTING_POSITIVE);
        int snapshotBytes = EvaluationJson.bytes(pending.configuration().configurationSnapshot());
        if (snapshotBytes + EvaluationJson.bytes(EvaluationJson.write(signals)) + 8192 > settings.recordBytes()) {
            reason = OutcomeReason.EVIDENCE_TOO_LARGE;
            complete = false;
            signals = List.of();
        }
        if (reason == null && strong && safe) reason = OutcomeReason.SIGNAL_CONFLICT;
        if (reason == null && !strong && redacted && (response.contains("[REDACTED_CREDENTIAL]")
                || signals.stream().anyMatch(signal -> signal.reasonCode().equals("EVIDENCE_REDACTED")))) {
            reason = OutcomeReason.EVIDENCE_INCOMPLETE;
            complete = false;
        }
        EvaluatorInput input = null;
        int inputBytes = 0;
        String projection = null;
        if (reason == null && !strong) {
            var corpus = exportCorpus.get(pending.id());
            if (!safe && !support) reason = OutcomeReason.INSUFFICIENT_EVIDENCE;
            else if (!snapshot.exportAuthorized()) reason = OutcomeReason.EXPORT_NOT_AUTHORIZED;
            else if (corpus == null || !corpus.exportableResponses().contains(response)) reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
            else if (bytes > settings.evidenceBytes()) reason = OutcomeReason.EVIDENCE_TOO_LARGE;
            else {
                var candidate = EvaluatorInput.semantic(criteria, SemanticProjection.from(criteria, test.prompt(), context, signals, response));
                String candidateProjection = messages.projection(candidate);
                // Inspeccionar los mismos campos que se envían en los mensajes developer y user.
                if (blocksSpanExport(EvaluationJson.parse(candidateProjection), MAX_EMBEDDED_JSON_LAYERS)
                        || containsBlocked(messages.inspectionView(candidate, settings), Set.of(INERT_SPAN))) reason = OutcomeReason.EVIDENCE_NOT_EXPORTABLE;
                else {
                    inputBytes = EvaluationJson.bytes(messages.request(candidate, settings));
                    if (inputBytes > settings.inputBytes() || snapshotBytes + EvaluationJson.bytes(candidateProjection)
                            + EvaluationJson.bytes(EvaluationJson.write(signals)) + 8192 > settings.recordBytes()) reason = OutcomeReason.EVIDENCE_TOO_LARGE;
                    else { input = candidate; projection = candidateProjection; }
                }
            }
        }
        var evidence = new EvaluationResult.Evidence(projection, complete, redacted, inputBytes, bytes,
                projection == null ? null : EvaluationJson.hash(projection));
        var component = input != null ? EvaluationResult.LlmComponent.pending()
                : new EvaluationResult.LlmComponent(LlmStatus.SKIPPED, null, 0, null, null, null, null, null, null, null, null, null, null);
        return new Prepared(pending.start(clock.instant(), execution.finishedAt(), signals, evidence, component),
                criteria, context, input, reason);
    }

    private static boolean blocksSpanExport(Object value, int layersRemaining) {
        return containsBlocked(value, Set.of(INERT_SPAN));
    }

    private static boolean matched(List<RuleSignal> signals, RuleSignal.Strength strength) {
        return signals.stream().anyMatch(signal -> signal.strength() == strength);
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
