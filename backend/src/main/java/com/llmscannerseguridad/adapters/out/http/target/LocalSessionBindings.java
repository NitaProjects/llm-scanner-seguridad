package com.llmscannerseguridad.adapters.out.http.target;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import com.llmscannerseguridad.domain.target.*;
import tools.jackson.databind.json.JsonMapper;

public final class LocalSessionBindings implements TargetIntegrationPort {
    private static final int FILE_BYTES = 262144;
    private static final int MAX_VALUE_BYTES = 4096;
    private static final Pattern SLOT = Pattern.compile("\\{\\{([A-Za-z][A-Za-z0-9]{0,31})}}");
    private static final Set<String> RESERVED = Set.of("accept", "authorization", "proxyauthorization", "apikey",
            "password", "credential", "credentials", "secret", "token", "cookie", "setcookie", "host", "origin",
            "contenttype", "contentlength", "connection", "transferencoding", "acceptencoding", "forwarded",
            "upgrade", "trailer", "te", "keepalive", "proxyconnection", "prompt", "metadata", "scanid", "projectid",
            "targetid", "executionid", "method", "url", "headers", "body", "source", "scope", "lifetime", "sensitive",
            "system", "internal");
    private final Path file;
    private final HttpTargetInvocationAdapter transport;
    private final StrictTargetJson json = new StrictTargetJson();
    private static final JsonTargetConfigurationValidator PATHS = new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));

    private record Binding(String method, String bootstrapPath, String bootstrapBody, String promptPath,
            List<InteractionSession.Capture> captures) { }

    public LocalSessionBindings(Path file, HttpTargetInvocationAdapter transport) {
        if (file != null && (file.toString().startsWith("\\\\") || file.getFileName() == null
                || !file.getFileName().toString().endsWith(".json"))) throw invalid();
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
            var root = object(json.read(text), Set.of("schemaVersion", "bindings"), Set.of());
            if (!Integer.valueOf(1).equals(root.get("schemaVersion")) || !(root.get("bindings") instanceof List<?> entries)
                    || entries.size() > 64) throw invalid();
            var seen = new HashSet<UUID>();
            Binding selected = null;
            for (Object item : entries) {
                var entry = object(item, Set.of("projectId", "targetConfigurationId", "targetUpdatedAt", "bootstrap", "captures"), Set.of("promptPath"));
                UUID id = uuid(entry, "targetConfigurationId"), project = uuid(entry, "projectId");
                Instant updated = Instant.parse(string(entry, "targetUpdatedAt", 64));
                if (!seen.add(id) || updated.getNano() % 1000 != 0) throw invalid();
                var binding = binding(entry);
                if (!target.id().equals(id)) continue;
                if (!target.projectId().equals(project) || !target.updatedAt().equals(updated)
                        || target.settings().adapterType() != TargetAdapterType.REST_LEGACY) throw invalid();
                String authHeader = target.settings().authHeaderName();
                if (authHeader != null && binding.captures().stream().anyMatch(c -> !c.sensitive()
                        && c.path().stream().anyMatch(segment -> segment instanceof String key && authHeader.equalsIgnoreCase(key)))) throw invalid();
                validateSlots(json.read(target.settings().requestTemplate()), binding.captures());
                selected = binding;
            }
            if (selected == null) return null;
            Binding fixed = selected;
            return (ignored, prompt, budget) -> interact(target, prompt, budget, fixed);
        } catch (Exception error) {
            org.slf4j.LoggerFactory.getLogger(LocalSessionBindings.class).warn("sessionBinding=TARGET_CONFIGURATION_INVALID");
            throw invalid();
        }
    }

    private NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget, Binding binding) {
        var base = target.settings().url();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(target.settings().timeoutMs());
        try (var session = new InteractionSession(base)) {
            var bootstrap = transport.invokeSession(target, prompt, budget, ignored -> binding.bootstrapBody(),
                    new HttpTargetInvocationAdapter.SessionExchange(binding.method(), destination(base, binding.bootstrapPath()), deadline,
                            (body, response) -> session.capture(transport.parseSessionResponse(body), response, binding.captures()), session));
            if (bootstrap.errorReason() != null) return failure(bootstrap.errorReason());
            TargetUrl finalUrl = binding.promptPath() == null ? base : destination(base, resolvePath(binding.promptPath(), session, base));
            return LegacyInvocationResultProjection.fromLegacy(transport.invokeSession(target, prompt, budget,
                    value -> transport.renderSession(target, value, slot -> resolveNode(slot, session, base)),
                    new HttpTargetInvocationAdapter.SessionExchange("POST", finalUrl, deadline, null, session)));
        } catch (InvocationFailure error) {
            return failure(error.reason);
        } catch (IllegalArgumentException error) {
            return failure(ExecutionError.TARGET_CONFIGURATION_INVALID);
        }
    }

    private Binding binding(Map<?, ?> entry) {
        var bootstrap = object(entry.get("bootstrap"), Set.of("method", "path"), Set.of("body"));
        String method = string(bootstrap, "method", 4), path = string(bootstrap, "path", 2048);
        if (!Set.of("GET", "POST").contains(method) || method.equals("GET") && bootstrap.containsKey("body")
                || bootstrap.containsKey("body") && !(bootstrap.get("body") instanceof Map<?, ?>)) throw invalid();
        validatePath(path, List.of());
        var body = JsonMapper.shared().writeValueAsString(bootstrap.containsKey("body") ? bootstrap.get("body") : Map.of());
        if (containsSlot(bootstrap.get("body"))) throw invalid();
        if (TargetRules.bytes(body) > 32768) throw invalid();
        var captures = parseCaptures(entry.get("captures"));
        if (captures.isEmpty()) throw invalid();
        String promptPath = entry.containsKey("promptPath") ? string(entry, "promptPath", 2048) : null;
        if (promptPath != null) validatePath(promptPath, captures);
        return new Binding(method, path, body, promptPath, captures);
    }

    static List<InteractionSession.Capture> parseCaptures(Object value) {
        if (!(value instanceof List<?> raw) || raw.size() > 16) throw invalid();
        var names = new HashSet<String>();
        var selectors = new HashSet<CaptureSelector>();
        var captures = new ArrayList<InteractionSession.Capture>();
        for (Object item : raw) {
            var declaration = object(item, Set.of("name", "source", "selector", "type", "required", "sensitive", "maxBytes"), Set.of());
            String name = string(declaration, "name", 32), selector = string(declaration, "selector", 1024);
            String lower = name.toLowerCase(Locale.ROOT);
            if (!name.matches("[A-Za-z][A-Za-z0-9]{0,31}") || reserved(lower) || !names.add(lower)) throw invalid();
            var source = InteractionSession.Source.valueOf(string(declaration, "source", 6));
            var type = InteractionSession.Type.valueOf(string(declaration, "type", 7));
            if (!(declaration.get("required") instanceof Boolean required) || !(declaration.get("sensitive") instanceof Boolean sensitive)
                    || !(declaration.get("maxBytes") instanceof Integer max) || max < 1 || max > MAX_VALUE_BYTES) throw invalid();
            List<Object> segments = source == InteractionSession.Source.JSON ? List.copyOf(PATHS.pathSegments(selector)) : List.of();
            Object canonical = switch (source) {
                case JSON -> segments;
                case HEADER -> selector.toLowerCase(Locale.ROOT);
                case COOKIE -> selector;
            };
            if (!selectors.add(new CaptureSelector(source, canonical))
                    || source != InteractionSession.Source.JSON && (!sensitive || type != InteractionSession.Type.STRING
                        || !selector.matches("[!#$%&'*+.^_\\x60|~0-9A-Za-z-]{1,128}"))
                    || source == InteractionSession.Source.HEADER && selector.equalsIgnoreCase("Set-Cookie")
                    || source == InteractionSession.Source.JSON && !sensitive && segments.stream()
                            .anyMatch(segment -> segment instanceof String key && reserved(key.replaceAll("[-_]", "").toLowerCase(Locale.ROOT)))) throw invalid();
            captures.add(new InteractionSession.Capture(name, source, selector, segments, type, max, required, sensitive));
        }
        return List.copyOf(captures);
    }

    private record CaptureSelector(InteractionSession.Source source, Object canonical) { }

    private static boolean reserved(String lower) {
        return RESERVED.contains(lower) || lower.startsWith("xforwarded") || lower.endsWith("apikey")
                || lower.endsWith("password") || lower.endsWith("authorization") || lower.endsWith("token")
                || lower.endsWith("secret");
    }

    private static boolean containsSlot(Object node) {
        if (node instanceof Map<?, ?> map) return map.entrySet().stream().anyMatch(e -> containsSlot(e.getKey()) || containsSlot(e.getValue()));
        if (node instanceof List<?> list) return list.stream().anyMatch(LocalSessionBindings::containsSlot);
        return node instanceof String text && hasBraces(text);
    }

    static void validateSlots(Object node, List<InteractionSession.Capture> captures) {
        if (node instanceof Map<?, ?> map) map.forEach((key, value) -> {
            if (containsSlot(key)) throw invalid();
            validateSlots(value, captures);
        });
        else if (node instanceof List<?> list) list.forEach(item -> validateSlots(item, captures));
        else if (node instanceof String text && hasBraces(text) && !text.equals(TargetSettings.PROMPT_PLACEHOLDER)) validateSlot(text, captures);
    }

    static void validatePath(String path, List<InteractionSession.Capture> captures) {
        if (!path.startsWith("/") || path.startsWith("//")) throw invalid();
        for (String segment : path.split("/", -1)) {
            if (hasBraces(segment)) validateSlot(segment, captures);
            else if (!segment.matches("[A-Za-z0-9._~-]*") || segment.equals(".") || segment.equals("..")) throw invalid();
        }
    }

    private static void validateSlot(String text, List<InteractionSession.Capture> captures) {
        var matcher = SLOT.matcher(text);
        if (!matcher.matches() || captures.stream().noneMatch(c -> c.name().equals(matcher.group(1))
                && c.source() == InteractionSession.Source.JSON && !c.sensitive())) throw invalid();
    }

    static String resolvePath(String path, InteractionSession session, TargetUrl base) {
        var resolved = new StringJoiner("/");
        for (String segment : path.split("/", -1)) {
            Object value = resolveNode(segment, session, base);
            if (hasBraces(segment)) {
                String text = value.toString();
                if (text.codePoints().anyMatch(c -> ":/?#[]@!$&'()*+,;=%\\".indexOf(c) >= 0)
                        || text.equals(".") || text.equals("..")) throw invalid();
                resolved.add(encodeSegment(text));
            } else resolved.add(segment);
        }
        return resolved.toString();
    }

    private static String encodeSegment(String text) {
        var encoded = new StringBuilder();
        for (byte value : text.getBytes(StandardCharsets.UTF_8)) {
            int b = Byte.toUnsignedInt(value);
            if (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9' || "-._~".indexOf(b) >= 0) encoded.append((char) b);
            else encoded.append('%').append(HexFormat.of().toHexDigits(value));
        }
        return encoded.toString();
    }

    static Object resolveNode(String text, InteractionSession session, TargetUrl base) {
        if (!hasBraces(text)) return text;
        var matcher = SLOT.matcher(text);
        if (!matcher.matches()) throw invalid();
        return session.identifier(matcher.group(1), base);
    }

    static TargetUrl destination(TargetUrl base, String path) {
        URI uri = URI.create(base.value());
        return new TargetUrl(uri.getScheme() + "://" + uri.getRawAuthority() + path);
    }

    private static boolean hasBraces(String text) { return text.contains("{{") || text.contains("}}"); }

    static Map<?, ?> object(Object value, Set<String> required, Set<String> optional) {
        if (!(value instanceof Map<?, ?> map) || !map.keySet().containsAll(required)
                || map.keySet().stream().anyMatch(key -> !required.contains(key) && !optional.contains(key))) throw invalid();
        return map;
    }

    static UUID uuid(Map<?, ?> map, String field) {
        String value = string(map, field, 36);
        var id = UUID.fromString(value);
        if (!id.toString().equalsIgnoreCase(value)) throw invalid();
        return id;
    }

    static String string(Map<?, ?> fields, String key, int max) {
        if (!(fields.get(key) instanceof String text) || text.isBlank() || text.length() > max
                || TargetRules.controls(text) || SecurityTest.invalidUnicode(text)) throw invalid();
        return text;
    }

    private static NormalizedInteraction failure(ExecutionError error) {
        return LegacyInvocationResultProjection.fromLegacy(InvocationResult.failure(error));
    }

    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Binding de sesión no válido."); }
}
