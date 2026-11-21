package com.llmscannerseguridad.privacy;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BooleanSupplier;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** N4: vistas efímeras para privacidad, nunca texto evaluable ni normalización de red. */
public final class ProtectedContentInspector {
    public static final int MAX_LAYERS = 4, MAX_DEPTH = 32, MAX_VALUES = 512, MAX_NODES = 4096;
    public static final String MARKER = "[REDACTED_CREDENTIAL]";
    public enum State { SAFE, MATCH, UNINSPECTABLE }
    public record Interval(int start, int end) { }
    public record Inspection(State state, List<Interval> intervals) {
        public Inspection { intervals = List.copyOf(intervals); }
        @Override public String toString() { return "Inspection[" + state + "]"; }
    }
    private record Frame(Object value, int layer) { }
    private static final JsonMapper JSON = JsonMapper.builder(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(MAX_DEPTH).build()).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
                    DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();
    private static final BooleanSupplier AVAILABLE = ProtectionScope::available;

    public Inspection inspect(String text, Collection<String> protectedValues, int maxBytes) {
        return inspect(text, protectedValues, maxBytes, AVAILABLE, false);
    }

    /** El wrapper propio se abre una vez; no consume una capa del contenido atacante. */
    public Inspection inspectDocument(String json, Collection<String> protectedValues, int maxBytes) {
        return inspect(json, protectedValues, maxBytes, AVAILABLE, true);
    }

    public Inspection inspect(String text, Collection<String> protectedValues, int maxBytes,
            BooleanSupplier available, boolean document) {
        try {
            if (maxBytes < 0 || protectedValues.size() > MAX_VALUES) throw incomplete();
            var patterns = new LinkedHashSet<String>();
            long patternBytes = 0;
            for (String value : protectedValues) {
                if (value == null || value.isEmpty()) continue;
                patternBytes += bytes(value);
                if (patternBytes > 524288) throw incomplete();
            }
            var patternWork = new Work(Math.max(1, patternBytes), available);
            for (String value : protectedValues) if (value != null && !value.isEmpty()) {
                // Validar todas las ramas no convierte las claves de un objeto protegido
                // en nuevos secretos independientes (por ejemplo, "query" o "id").
                walk(value, false, patternWork, ignored -> { });
                String current = value;
                for (int layer = 0; layer <= MAX_LAYERS; layer++) {
                    patternWork.view(current); patterns.add(current);
                    String leading = current.stripLeading();
                    String next = current;
                    if (!leading.isEmpty() && !leading.startsWith(MARKER) && !placeholder(leading)
                            && "\"{[".indexOf(leading.charAt(0)) >= 0) {
                        Object parsed = JSON.readValue(current, Object.class);
                        next = parsed instanceof String string ? string : JSON.writeValueAsString(parsed);
                    } else if (current.indexOf('\\') >= 0 || quotedFragment(current)) next = decodeFreeText(current);
                    if (next.equals(current)) break;
                    if (layer == MAX_LAYERS) throw incomplete();
                    current = next;
                }
            }
            if (text == null) return new Inspection(State.SAFE, List.of());
            if (bytes(text) > maxBytes) throw incomplete();
            var intervals = new ArrayList<Interval>();
            for (String value : patterns) {
                available(available);
                for (int at = text.indexOf(value); at >= 0; at = text.indexOf(value, at + 1)) {
                    if (intervals.size() == MAX_NODES) throw incomplete();
                    intervals.add(new Interval(at, at + value.length()));
                }
            }
            boolean[] matched = { !intervals.isEmpty() };
            var work = new Work(maxBytes, available);
            walk(text, document, work, candidate -> {
                for (String pattern : patterns) {
                    available(available);
                    if (candidate.contains(pattern)) matched[0] = true;
                }
            });
            // No retorno temprano por MATCH: cualquier rama incompleta prevalece.
            return new Inspection(matched[0] ? State.MATCH : State.SAFE, merge(intervals));
        } catch (RuntimeException error) {
            return new Inspection(State.UNINSPECTABLE, List.of());
        }
    }

    private static void walk(String original, boolean document, Work work, java.util.function.Consumer<String> visit) {
        validate(original);
        var pending = new ArrayDeque<Frame>();
        if (document) {
            pending.push(new Frame(JSON.readValue(original, Object.class), 0));
        } else pending.push(new Frame(original, 0));
        var seen = new HashMap<String, Integer>();
        while (!pending.isEmpty()) {
            available(work.available);
            if (++work.nodes > MAX_NODES) throw incomplete();
            var frame = pending.pop();
            Object value = frame.value();
            if (value instanceof Map<?, ?> map) {
                for (var entry : map.entrySet()) {
                    push(pending, entry.getKey(), frame.layer()); push(pending, entry.getValue(), frame.layer());
                }
            } else if (value instanceof List<?> list) {
                for (Object item : list) push(pending, item, frame.layer());
            } else if (value != null) {
                String text = value.toString();
                validate(text);
                // Revisitar con mayor profundidad sigue siendo necesario para detectar agotamiento.
                Integer previous = seen.put(text, frame.layer());
                if (previous != null && previous >= frame.layer()) continue;
                work.view(text); visit.accept(text);
                String leading = text.stripLeading();
                // El marcador fijo es texto de presentación, no una lista JSON. Sigue pasando
                // por matching y escapes: no está exceptuado si él mismo es un valor protegido.
                boolean jsonCandidate = !leading.isEmpty() && !leading.startsWith(MARKER) && !placeholder(leading)
                        && "\"{[".indexOf(leading.charAt(0)) >= 0;
                if (jsonCandidate) {
                    Object parsed = JSON.readValue(text, Object.class);
                    boolean decoding = parsed instanceof String || text.indexOf('\\') >= 0;
                    if (decoding && frame.layer() == MAX_LAYERS) throw incomplete();
                    push(pending, parsed, frame.layer() + (decoding ? 1 : 0));
                } else if (text.indexOf('\\') >= 0 || quotedFragment(text)) {
                    if (frame.layer() == MAX_LAYERS) {
                        // Detectar transformación sin construir/usar una quinta vista.
                        throw incomplete();
                    }
                    String next = decodeFreeText(text);
                    if (!next.equals(text)) push(pending, next, frame.layer() + 1);
                }
            }
        }
    }

    private static void push(ArrayDeque<Frame> pending, Object node, int layer) {
        if (pending.size() >= MAX_NODES) throw incomplete();
        pending.push(new Frame(node, layer));
    }

    private static boolean quoteStart(String text, int index) {
        return text.charAt(index) == '"' && (index == 0 || Character.isWhitespace(text.charAt(index - 1))
                || ":,[{".indexOf(text.charAt(index - 1)) >= 0);
    }
    private static boolean placeholder(String text) {
        if (!text.startsWith("{{") || !text.endsWith("}}") || text.length() < 5 || text.length() > 36) return false;
        for (int i = 2; i < text.length() - 2; i++) {
            char c = text.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }
    private static int quoteEnd(String text, int start) {
        for (int i = start + 1; i < text.length(); i++) {
            if (text.charAt(i) == '\\') i++;
            else if (text.charAt(i) == '"') return i;
        }
        return -1;
    }
    private static boolean quotedFragment(String text) {
        for (int i = 0; i < text.length(); i++) if (quoteStart(text, i) && quoteEnd(text, i) >= 0) return true;
        return false;
    }
    private static String decodeFreeText(String text) {
        var out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (quoteStart(text, i)) {
                int end = quoteEnd(text, i);
                if (end >= 0) {
                    // El escáner delimita tokens; Jackson sigue siendo el único parser JSON.
                    out.append(JSON.readValue(text.substring(i, end + 1), String.class)); i = end; continue;
                }
            }
            if (text.charAt(i) == '\\') {
                if (i + 1 >= text.length()) throw incomplete();
                int end = i + (text.charAt(i + 1) == 'u' ? 6 : 2);
                if (end > text.length()) throw incomplete();
                validateEscapes(text.substring(i, end), out); i = end - 1;
            } else out.append(text.charAt(i));
        }
        return out.toString();
    }

    private static void validateEscapes(String text, StringBuilder out) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                if (++i >= text.length()) throw incomplete();
                char escape = text.charAt(i);
                c = switch (escape) {
                    case '"', '\\', '/' -> escape;
                    case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                    case 'u' -> {
                        if (i + 4 >= text.length()) throw incomplete();
                        int code = 0;
                        for (int j = 0; j < 4; j++) {
                            int digit = Character.digit(text.charAt(++i), 16);
                            if (digit < 0 || text.charAt(i) > 127) throw incomplete();
                            code = code * 16 + digit;
                        }
                        yield (char) code;
                    }
                    default -> throw incomplete();
                };
            }
            if (out != null) out.append(c);
        }
    }

    public String redact(String text, Collection<String> protectedValues, int maxBytes) {
        var result = inspect(text, protectedValues, maxBytes);
        if (result.state() == State.UNINSPECTABLE) throw incomplete();
        if (result.state() == State.SAFE) return text;
        String safe;
        if (result.intervals().isEmpty()) safe = MARKER;
        else {
            var output = new StringBuilder(); int cursor = 0;
            for (var interval : result.intervals()) {
                output.append(text, cursor, interval.start()).append(MARKER); cursor = interval.end();
            }
            safe = output.append(text, cursor, text.length()).toString();
        }
        var checked = inspect(safe, protectedValues, maxBytes);
        if (checked.state() != State.SAFE) safe = MARKER;
        if (inspect(safe, protectedValues, maxBytes).state() != State.SAFE) throw incomplete();
        return safe;
    }

    public String redactDocument(String document, Collection<String> values, int maxBytes) {
        var state = inspectDocument(document, values, maxBytes).state();
        if (state == State.UNINSPECTABLE) throw incomplete();
        if (state == State.SAFE) return document;
        Object safe = redactNode(JSON.readValue(document, Object.class), values, maxBytes);
        String encoded = JSON.writeValueAsString(safe);
        if (inspectDocument(encoded, values, maxBytes).state() != State.SAFE) throw incomplete();
        return encoded;
    }
    private Object redactNode(Object node, Collection<String> values, int maxBytes) {
        if (node instanceof Map<?, ?> map) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : map.entrySet()) {
                String key = redact(entry.getKey().toString(), values, maxBytes);
                if (copy.containsKey(key)) throw incomplete();
                copy.put(key, redactNode(entry.getValue(), values, maxBytes));
            }
            return copy;
        }
        if (node instanceof List<?> list) return list.stream().map(item -> redactNode(item, values, maxBytes)).toList();
        if (node == null) return null;
        String value = node.toString(), safe = redact(value, values, maxBytes);
        return safe.equals(value) ? node : safe;
    }

    private static List<Interval> merge(List<Interval> input) {
        input.sort(Comparator.comparingInt(Interval::start));
        var result = new ArrayList<Interval>();
        for (var interval : input) {
            if (!result.isEmpty() && interval.start() <= result.getLast().end()) {
                var prior = result.removeLast(); result.add(new Interval(prior.start(), Math.max(prior.end(), interval.end())));
            } else result.add(interval);
        }
        return result;
    }

    private static final class Work {
        private final long maxViews; private final BooleanSupplier available;
        private final Set<String> counted = new HashSet<>();
        private long views; private int nodes;
        Work(long bytes, BooleanSupplier available) { maxViews = 5L * bytes; this.available = available; }
        void view(String text) { if (counted.add(text)) views += bytes(text); if (views > maxViews) throw incomplete(); }
    }
    private static void available(BooleanSupplier available) { if (!available.getAsBoolean()) throw incomplete(); }
    private static void validate(String text) { if (SecurityTest.invalidUnicode(text)) throw incomplete(); }
    private static int bytes(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
    public static IllegalStateException incomplete() { return new IllegalStateException("Inspección de privacidad incompleta."); }
}
