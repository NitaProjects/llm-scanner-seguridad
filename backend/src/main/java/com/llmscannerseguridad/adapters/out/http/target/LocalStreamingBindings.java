package com.llmscannerseguridad.adapters.out.http.target;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.llmscannerseguridad.adapters.out.validation.target.JsonTargetConfigurationValidator;
import com.llmscannerseguridad.adapters.out.validation.target.LocalTargetUrlPolicy;
import com.llmscannerseguridad.adapters.out.validation.target.StrictTargetJson;
import com.llmscannerseguridad.application.execution.LegacyInvocationResultProjection;
import com.llmscannerseguridad.application.execution.TargetAdapter;
import com.llmscannerseguridad.application.execution.TargetIntegrationPort;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import org.slf4j.LoggerFactory;

public final class LocalStreamingBindings implements TargetIntegrationPort {
    private static final int FILE_BYTES = 262144;
    private static final Set<String> MEDIA = Set.of("text/event-stream", "application/x-ndjson", "application/jsonl");
    private final Path file;
    private final HttpTargetInvocationAdapter transport;
    private static final StrictTargetJson JSON = new StrictTargetJson();
    private static final JsonTargetConfigurationValidator PATHS = new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));

    public LocalStreamingBindings(Path file, HttpTargetInvocationAdapter transport) {
        if (file != null && (file.toString().startsWith("\\\\") || !file.getFileName().toString().endsWith(".json"))) throw invalid();
        this.file = file;
        this.transport = transport;
    }

    @Override public TargetAdapter resolve(TargetConfiguration target) {
        if (file == null) return null;
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(FILE_BYTES + 1);
            if (bytes.length > FILE_BYTES) throw invalid();
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            var root = object(JSON.read(text), Set.of("schemaVersion", "bindings"), Set.of());
            if (!Integer.valueOf(1).equals(root.get("schemaVersion")) || !(root.get("bindings") instanceof List<?> entries)
                    || entries.size() > 64) throw invalid();
            StreamBinding selected = null;
            ObservationBinding observations = null;
            boolean selectedJson = false;
            var targets = new HashSet<UUID>();
            for (Object item : entries) {
                var entry = object(item, Set.of("projectId", "targetConfigurationId", "targetUpdatedAt", "mediaType"),
                        Set.of("textPath", "mode", "eventPath", "contentEvent", "errorEvent", "terminalEvent",
                                "terminalLiteral", "reservedHeaders", "observations"));
                UUID project = uuid(entry, "projectId");
                UUID targetId = uuid(entry, "targetConfigurationId");
                Instant updated = Instant.parse(required(entry, "targetUpdatedAt"));
                if (!targets.add(targetId) || updated.getNano() % 1000 != 0) throw invalid();
                boolean fullJson = "application/json".equals(entry.get("mediaType"));
                if (fullJson && (entry.size() != 5 || !entry.containsKey("observations"))) throw invalid();
                StreamBinding binding = fullJson ? null : binding(entry);
                var profile = entry.containsKey("observations") ? ObservationBinding.parse(entry.get("observations"), binding, true) : null;
                if (!targetId.equals(target.id())) continue;
                if (!project.equals(target.projectId()) || !updated.equals(target.updatedAt())
                        || binding != null && !binding.permits(target.settings())) throw invalid();
                selected = binding;
                selectedJson = fullJson;
                observations = profile;
            }
            if (selected == null && !selectedJson) return null;
            StreamBinding fixed = selected;
            ObservationBinding profile = observations;
            if (profile != null) return (current, prompt, budget) -> transport.invokeObserved(target, prompt, budget, fixed, profile);
            return (current, prompt, budget) -> LegacyInvocationResultProjection.fromLegacy(transport.invokeStream(target, prompt, budget, fixed));
        } catch (Exception error) {
            LoggerFactory.getLogger(LocalStreamingBindings.class).warn("streamBinding=TARGET_CONFIGURATION_INVALID");
            throw invalid();
        }
    }

    static StreamBinding binding(Map<?, ?> entry) {
        String media = required(entry, "mediaType");
        if (!MEDIA.contains(media)) throw invalid();
        String eventPath = optional(entry, "eventPath");
        String content = optional(entry, "contentEvent");
        String error = optional(entry, "errorEvent");
        String terminal = optional(entry, "terminalEvent");
        String literal = optional(entry, "terminalLiteral");
        if ((terminal == null) == (literal == null) || terminal != null && (terminal.equals(content) || terminal.equals(error))
                || error != null && error.equals(content) || media.equals("text/event-stream") && eventPath != null
                || !media.equals("text/event-stream") && eventPath == null && (content != null || error != null || terminal != null)) throw invalid();
        if (!media.equals("text/event-stream") && literal != null && !(JSON.read(literal) instanceof Map<?, ?>)) throw invalid();
        var reserved = new HashSet<String>();
        if (entry.containsKey("reservedHeaders")) {
            if (!(entry.get("reservedHeaders") instanceof List<?> names) || names.size() > 20) throw invalid();
            for (var name : names) {
                if (!(name instanceof String header) || !header.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}")) throw invalid();
                if (!reserved.add(header.toLowerCase(java.util.Locale.ROOT))) throw invalid();
            }
        }
        return new StreamBinding(media, eventPath == null ? null : List.copyOf(PATHS.pathSegments(eventPath)),
                List.copyOf(PATHS.pathSegments(required(entry, "textPath"))), content, error, terminal, literal,
                StreamBinding.Mode.valueOf(required(entry, "mode")), Set.copyOf(reserved));
    }

    private static Map<?, ?> object(Object value, Set<String> required, Set<String> optional) {
        if (!(value instanceof Map<?, ?> map) || !map.keySet().containsAll(required)
                || map.keySet().stream().anyMatch(key -> !required.contains(key) && !optional.contains(key))) throw invalid();
        return map;
    }

    private static UUID uuid(Map<?, ?> fields, String key) {
        String text = required(fields, key);
        UUID id = UUID.fromString(text);
        if (!id.toString().equalsIgnoreCase(text)) throw invalid();
        return id;
    }

    private static String optional(Map<?, ?> fields, String key) {
        return fields.containsKey(key) ? required(fields, key) : null;
    }

    private static String required(Map<?, ?> fields, String key) {
        if (!(fields.get(key) instanceof String text) || text.isBlank() || text.length() > 1024 || text.indexOf('\0') >= 0
                || com.llmscannerseguridad.domain.securitytest.SecurityTest.invalidUnicode(text)) throw invalid();
        return text;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Binding streaming no válido.");
    }
}
