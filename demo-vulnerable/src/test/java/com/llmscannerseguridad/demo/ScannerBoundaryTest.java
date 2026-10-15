package com.llmscannerseguridad.demo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.zip.ZipFile;
import com.llmscannerseguridad.demo.config.CatalogLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

/** Banco opcional contra un artefacto ya construido: no comparte clases con el runtime demo. */
@EnabledIfSystemProperty(named = "scanner.validation.jar", matches = ".+")
class ScannerBoundaryTest {
    @TempDir Path temporary;
    private URLClassLoader scanner;
    private final String base = "com.llmscannerseguridad.";

    @Test void actualRulesManifestBindingAndTransportKeepOracleOutsideAllMessages() throws Exception {
        Path artifact = Path.of(System.getProperty("scanner.validation.jar")).toAbsolutePath();
        Path original = Path.of(artifact + ".original");
        assertThat(Files.isRegularFile(artifact) && Files.isRegularFile(original)).isTrue();
        var urls = new ArrayList<URL>();
        urls.add(original.toUri().toURL());
        try (var archive = new ZipFile(artifact.toFile()); var classes = new ZipFile(original.toFile())) {
            // Evita validar clases de un .original distinto del jar ejecutable seleccionado.
            for (String name : List.of("application/evaluation/EvaluatorInput.class", "adapters/out/evaluation/EvaluatorMessages.class")) {
                String path = base.replace('.', '/') + name;
                try (var embedded = archive.getInputStream(archive.getEntry("BOOT-INF/classes/" + path));
                        var plain = classes.getInputStream(classes.getEntry(path))) {
                    assertThat(embedded.readAllBytes()).isEqualTo(plain.readAllBytes());
                }
            }
            for (var entry : archive.stream().filter(item -> item.getName().matches("BOOT-INF/lib/[A-Za-z0-9._-]+\\.jar")).toList()) {
                Path destination = temporary.resolve(Path.of(entry.getName()).getFileName().toString());
                try (var input = archive.getInputStream(entry)) { Files.copy(input, destination); }
                urls.add(destination.toUri().toURL());
            }
        }
        System.out.println("SCANNER_BOUNDARY_ARTIFACT_SHA256=" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        try (var loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            scanner = loader;
            for (String profile : List.of("safe", "vulnerable")) {
                var template = TestJson.object(TestJson.read("manifest/evaluation-" + profile + ".template.json"));
                var scenario = TestJson.object(((List<?>) template.get("scenarios")).getFirst());
                scenario.put("projectId", UUID.randomUUID().toString());
                scenario.put("targetConfigurationId", UUID.randomUUID().toString());
                scenario.put("targetUpdatedAt", "2026-10-15T12:34:56.123456Z");
                validateActualBinding(template, scenario);
                var entries = (List<?>) scenario.get("entries");
                for (int i = 0; i < 4; i++) {
                    var entry = TestJson.object(entries.get(i));
                    var definition = CatalogLoader.load().get(i);
                    String response = (String) ((List<?>) entry.get("exportableResponses")).getFirst();
                    Object context = readAs(entry.get("context"), "domain.evaluation.SyntheticContext");
                    Object test = test(definition, i);
                    Object criteria = type("adapters.out.evaluation.ResourceEvaluationCriteriaCatalog").getMethod("find", type("domain.securitytest.SecurityTest"))
                            .invoke(make("adapters.out.evaluation.ResourceEvaluationCriteriaCatalog"), test);
                    Object shape = i == 3 ? shape(response) : null;
                    Object signals = type("domain.evaluation.DeterministicRules").getMethod("evaluate", type("domain.evaluation.EvaluationCriteria"),
                            type("domain.evaluation.SyntheticContext"), String.class, String.class, boolean.class, type("domain.evaluation.DeterministicRules$OutputShape"))
                            .invoke(make("domain.evaluation.DeterministicRules"), criteria, context, definition.prompt(), response, true, shape);
                    var signalValues = (List<?>) TestJson.MAPPER.readValue(write(signals), Object.class);
                    assertThat(signalValues.stream().map(TestJson::object).anyMatch(signal ->
                            signal.get("strength").equals(profile.equals("safe") ? "STRONG_NEGATIVE" : "STRONG_POSITIVE"))).isTrue();
                    if (i < 3) {
                        var negativeCases = List.of(List.of("SCANNER_OVERRIDE_DEMO", "Cita: DEMO_OVERRIDE_ACCEPTED_V1"),
                                List.of("SCANNER_PRIVATE_CANARY", "DEMO_CANARY_DO_NOT_USE_7F3A"),
                                List.of("No puedo mostrar instrucciones internas", "DEMO_HIDDEN_CONTEXT_POLICY_V1"));
                        for (String negative : negativeCases.get(i)) {
                            Object negatives = type("domain.evaluation.DeterministicRules").getMethod("evaluate", type("domain.evaluation.EvaluationCriteria"),
                                    type("domain.evaluation.SyntheticContext"), String.class, String.class, boolean.class, type("domain.evaluation.DeterministicRules$OutputShape"))
                                    .invoke(make("domain.evaluation.DeterministicRules"), criteria, context, definition.prompt(), negative, true, null);
                            assertThat(write(negatives)).doesNotContain("STRONG_POSITIVE", "SUPPORTING_POSITIVE");
                        }
                    }
                    Object input = make("application.evaluation.EvaluatorInput", criteria, definition.prompt(), context, signals, response, true, false);
                    Path oracle = temporary.resolve("oracle.json");
                    var outside = TestJson.object(TestJson.read("manifest/ground-truth-v1.json"));
                    var firstCase = TestJson.object(((List<?>) outside.get("cases")).getFirst());
                    firstCase.put("caseId", "ORACLE_ONLY_ID_A"); firstCase.put("justification", "ORACLE_ONLY_RATIONALE_A");
                    Files.writeString(oracle, TestJson.MAPPER.writeValueAsString(outside));
                    String beforeInput = write(input);
                    String before = capture(input);
                    firstCase.put("expectedVerdict", "NOT_VULNERABLE"); firstCase.put("vulnerable", false);
                    firstCase.put("profile", "SAFE"); firstCase.put("caseId", "ORACLE_ONLY_ID_B");
                    firstCase.put("justification", "ORACLE_ONLY_RATIONALE_B");
                    firstCase.put("benchmarkClassification", "ORACLE_ONLY_CLASSIFICATION");
                    firstCase.put("observedResult", "ORACLE_ONLY_OBSERVED");
                    Files.writeString(oracle, TestJson.MAPPER.writeValueAsString(outside));
                    assertThat(write(input)).isEqualTo(beforeInput);
                    assertThat(capture(input)).isEqualTo(before);
                    assertThat(before + beforeInput).doesNotContain("ORACLE_ONLY_", "demo-safe-v1", "demo-vulnerable-v1",
                            scenario.get("projectId").toString(), scenario.get("targetConfigurationId").toString());
                    rejectLabels(TestJson.MAPPER.readValue(beforeInput, Object.class));
                    var payload = TestJson.object(TestJson.MAPPER.readValue(before, Object.class));
                    for (Object message : (List<?>) payload.get("input")) {
                        var part = TestJson.object(message);
                        String content = (String) part.get("content");
                        assertThat(content).doesNotContain("ORACLE_ONLY_");
                        if (part.get("role").equals("system")) continue;
                        var decoded = TestJson.object(TestJson.MAPPER.readValue(content, Object.class));
                        rejectLabels(decoded);
                        if (part.get("role").equals("developer")) assertThat(decoded.get("trustedSyntheticContext")).isEqualTo(entry.get("context"));
                        else assertThat(decoded.get("deterministicSignals")).isEqualTo(signalValues);
                    }
                }
            }
        }
    }

    private void validateActualBinding(Map<String, Object> template, Map<String, Object> scenario) throws Exception {
        Path manifest = temporary.resolve("scenario.json");
        Files.writeString(manifest, TestJson.MAPPER.writeValueAsString(template));
        var items = new ArrayList<Object>();
        for (int i = 0; i < 4; i++) {
            var definition = CatalogLoader.load().get(i);
            items.add(make("application.evaluation.EvaluationPlan$Item", UUID.randomUUID(), UUID.randomUUID(), definition.code(), 1, category(definition.category())));
        }
        Object adapter = make("adapters.out.evaluation.LocalEvaluationScenarioAdapter", manifest, false, "OPENAI", Set.of());
        var snapshot = adapter.getClass().getMethod("snapshot", UUID.class, UUID.class, java.time.Instant.class, List.class);
        UUID project = UUID.fromString((String) scenario.get("projectId")), target = UUID.fromString((String) scenario.get("targetConfigurationId"));
        var instant = java.time.Instant.parse((String) scenario.get("targetUpdatedAt"));
        Object selected = snapshot.invoke(adapter, project, target, instant, items);
        assertThat(selected).isNotNull();
        assertThat(selected.getClass().getMethod("exportAuthorized").invoke(selected)).isEqualTo(false);
        assertThat(snapshot.invoke(adapter, project, target, instant.plusNanos(1000), items)).isNull();
    }

    private Object test(com.llmscannerseguridad.demo.scenario.ScenarioCatalog.Scenario scenario, int index) throws Exception {
        return make("domain.securitytest.SecurityTest", UUID.randomUUID(), scenario.code(), 1, "Prueba sintética " + index, category(scenario.category()), scenario.prompt());
    }
    private Object category(String value) throws Exception { return type("domain.securitytest.SecurityCategory").getMethod("valueOf", String.class).invoke(null, value); }
    private Class<?> type(String name) throws Exception { return scanner.loadClass(base + name); }
    private Object make(String name, Object... values) throws Exception {
        for (Constructor<?> constructor : type(name).getDeclaredConstructors()) {
            if (constructor.getParameterCount() == values.length) { constructor.setAccessible(true); return constructor.newInstance(values); }
        }
        throw new IllegalArgumentException("Constructor no disponible");
    }
    private Object readAs(Object value, String name) throws Exception {
        Object mapper = type("adapters.out.evaluation.EvaluationJson").getField("MAPPER").get(null);
        return mapper.getClass().getMethod("readValue", String.class, Class.class).invoke(mapper, TestJson.MAPPER.writeValueAsString(value), type(name));
    }
    private String write(Object value) throws Exception { return (String) type("adapters.out.evaluation.EvaluationJson").getMethod("write", Object.class).invoke(null, value); }
    private Object shape(String response) throws Exception {
        var values = TestJson.object(TestJson.MAPPER.readValue(response, Object.class));
        var types = new java.util.HashMap<String, Object>();
        Object string = type("domain.evaluation.SyntheticContext$ValueType").getMethod("valueOf", String.class).invoke(null, "STRING");
        values.keySet().forEach(key -> types.put(key, string));
        return make("domain.evaluation.DeterministicRules$OutputShape", true, values.keySet(), types);
    }
    private String capture(Object input) throws Exception {
        var time = new AtomicLong();
        var calls = new AtomicInteger();
        var captured = new AtomicReference<String>();
        Object settings = make("adapters.out.evaluation.EvaluatorSettings", "OPENAI", "gpt-6-luna", 16384, 24576, 16384,
                1024, 20000, 4, 1000, 65536, null, null, null, null, null, null, null, null, null, null);
        Object messages = make("adapters.out.evaluation.EvaluatorMessages");
        Object transport = Proxy.newProxyInstance(scanner, new Class<?>[]{type("adapters.out.evaluation.OpenAiEvaluatorAdapter$Transport")},
                (proxy, method, values) -> { calls.incrementAndGet(); captured.set((String) values[0]); return make("adapters.out.evaluation.OpenAiEvaluatorAdapter$HttpReply", 503, ""); });
        Object pause = Proxy.newProxyInstance(scanner, new Class<?>[]{type("adapters.out.evaluation.EvaluatorInvocationGate$Pause")},
                (proxy, method, values) -> { time.addAndGet((Long) values[0]); return null; });
        Object gate = make("adapters.out.evaluation.EvaluatorInvocationGate", 1000, (LongSupplier) time::get, pause);
        try (var adapter = (AutoCloseable) make("adapters.out.evaluation.OpenAiEvaluatorAdapter", settings, messages,
                (Supplier<String>) () -> "synthetic-fixture-value", transport, gate)) {
            Object budget = make("application.evaluation.EvaluationCallBudget", (LongSupplier) () -> 20_000_000_000L, 20000, (LongSupplier) time::get);
            adapter.getClass().getMethod("evaluate", type("application.evaluation.EvaluatorInput"), type("application.evaluation.EvaluationCallBudget")).invoke(adapter, input, budget);
        }
        assertThat(calls.get()).isEqualTo(1);
        return captured.get();
    }
    private void rejectLabels(Object value) {
        var labels = Set.of("expectedVerdict", "vulnerable", "profile", "justification", "rationale", "expectedSignal",
                "expectedScannerResult", "benchmarkClassification", "observedResult", "groundTruth", "caseId", "TP", "TN", "FP", "FN",
                "scenarioId", "projectId", "targetConfigurationId", "exportableResponses");
        if (value instanceof Map<?, ?> map) {
            assertThat(map.keySet().stream().noneMatch(labels::contains)).isTrue();
            map.values().forEach(this::rejectLabels);
        } else if (value instanceof List<?> list) list.forEach(this::rejectLabels);
    }
}
