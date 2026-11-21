package com.llmscannerseguridad.privacy;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

class ProtectedContentInspectorTest {
    private static final String SECRET = "PRIVATE_42";
    private static final String SLASH = Character.toString(92);
    private final ProtectedContentInspector inspector = new ProtectedContentInspector();
    static String encode(String text, int count) {
        for (int i = 0; i < count; i++) text = JsonMapper.shared().writeValueAsString(text);
        return text;
    }
    static Stream<String> equivalents() {
        return Stream.of(SECRET, SLASH + "u0050RIVATE_42", SLASH + "u0050" + SLASH + "u0052IVATE_42",
                "PRIVATE" + SLASH + "u005f42", "PRIVATE" + SLASH + "u005F42", encode(SECRET, 1),
                encode(SECRET, 2), encode(SECRET, 4), "Nota: " + encode(SLASH + "u0050RIVATE_42", 2),
                "{\"inner\":" + encode(encode(SECRET, 2), 1) + "}");
    }
    @ParameterizedTest @MethodSource("equivalents")
    void t01ToT08FindsRuntimeN4Equivalents(String input) {
        assertThat(inspector.inspect(input, Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        String safe = inspector.redact(input, Set.of(SECRET), 65536);
        assertThat(safe).doesNotContain(SECRET);
        assertThat(inspector.inspect(safe, Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.redact(safe, Set.of(SECRET), 65536)).isEqualTo(safe);
    }
    @Test void t09DepthFiveAndFreeTextExhaustionAreNotSafeEvenWithoutFindingSecret() {
        for (String input : List.of(encode(SECRET, 5), "Nota: " + encode("a\"b", 7), encode("benign", 5))) {
            assertThat(inspector.inspect(input, Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        }
    }
    @Test void t10MalformedEscapesAndOtherBranchesPrecedeEarlyMatch() {
        for (String bad : List.of(SLASH + "u00", SLASH + "q", SLASH, encode(SECRET, 1) + " trailing",
                "{\"x\":1,\"x\":2}", "{\"safe\":\"" + SECRET + "\",\"bad\":\"" + SLASH + "u00\"}")) {
            assertThat(inspector.inspect(bad, Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        }
    }
    @Test void t11T12OriginalIntervalsOverlapAndOrderAreEquivalent() {
        String raw = "prefix-abc123-suffix prefix-abc123-suffix abc123 ABCDE";
        var values = List.of("abc123", "prefix-abc123-suffix", "ABC", "CDE");
        var reverse = new ArrayList<>(values); Collections.reverse(reverse);
        String expected = "[REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL] [REDACTED_CREDENTIAL]";
        assertThat(inspector.redact(raw, values, 65536)).isEqualTo(expected);
        assertThat(inspector.redact(raw, reverse, 65536)).isEqualTo(expected);
    }
    @Test void encodedOnlyMatchReplacesTheEntireFieldAndMarkerCollisionFailsClosed() {
        assertThat(inspector.redact("prefix " + SLASH + "u0050RIVATE_42 suffix", Set.of(SECRET), 65536))
                .isEqualTo(ProtectedContentInspector.MARKER);
        assertThatThrownBy(() -> inspector.redact(SECRET, Set.of(SECRET, "REDACTED"), 65536)).isInstanceOf(IllegalStateException.class);
    }
    @Test void exactCaseAndOrdinaryJsonAndProseArePreserved() {
        for (String text : List.of("private_42", "Texto normal con comillas \"cita\" y sin secretos",
                "{\"text\":\"say \\\"hello\\\"\",\"number\":17}", "Nota: " + encode("texto normal", 1), "")) {
            assertThat(inspector.inspect(text, Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
            assertThat(inspector.redact(text, Set.of(SECRET), 65536)).isEqualTo(text);
        }
    }
    @Test void unicodeSurrogatesControlsAndOwnStorageWrapper() {
        String emoji = new String(Character.toChars(0x1f600));
        assertThat(inspector.inspect(SLASH + "uD83D" + SLASH + "uDE00", Set.of(emoji), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        assertThat(inspector.inspect(SLASH + "uD83D", Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspect(SLASH + "u0000", Set.of("\0"), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        assertThat(inspector.inspectDocument(encode(encode(SECRET, 4), 1), Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
    }
    @Test void limitsAndInterruptionAreFailClosed() {
        assertThat(inspector.inspect("é", Set.of(), 2).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.inspect("é", Set.of(), 1).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspect("[".repeat(33) + "0" + "]".repeat(33), Set.of(), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspect("[".repeat(32) + "0" + "]".repeat(32), Set.of(), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.inspect("safe", java.util.stream.IntStream.range(0, 513).mapToObj(i -> "s" + i).toList(), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspect("x".repeat(4097), Set.of("x"), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspect("safe", Set.of(), 65536, () -> false, false).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
    }
    @Test void t02AllStandardJsonEscapesAndMixedHexAreInspected() {
        for (String secret : List.of("private\"quote", "private/slash", "private\\ntail", "private\bvalue",
                "private\fvalue", "private\nvalue", "private\rvalue", "private\tvalue")) {
            String encoded = encode(secret, 1);
            assertThat(inspector.inspect(encoded, Set.of(secret), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
            assertThat(inspector.redact(encoded, Set.of(secret), 65536)).contains(ProtectedContentInspector.MARKER).doesNotContain(secret);
        }
        assertThat(inspector.inspect("abc" + SLASH + "u00aF", Set.of("abc¯"), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
    }
    @Test void exactNodePatternAndIntervalLimitsAndScalarKeys() {
        var patterns = java.util.stream.IntStream.range(0, 512).mapToObj(i -> "fixture-" + i).toList();
        assertThat(inspector.inspect("benign", patterns, 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.inspect("x".repeat(4096), Set.of("x"), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        String nodes = "[" + String.join(",", Collections.nCopies(4094, "0")) + "]";
        assertThat(inspector.inspect(nodes, Set.of(), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.inspect(nodes.replace("]", ",0]"), Set.of(), 65536).state()).isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        assertThat(inspector.inspectDocument("{\"12345\":12345}", Set.of("12345"), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        assertThat(inspector.redactDocument("{\"key\":12345}", Set.of("12345"), 65536)).contains(ProtectedContentInspector.MARKER).doesNotContain("12345");
    }
    @Test void protectedJsonDoesNotPromoteItsGenericKeysToSeparateSecrets() {
        String protectedJson = "{\"query\":\"fixture-private\"}";
        assertThat(inspector.inspect(protectedJson, Set.of(protectedJson), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
        assertThat(inspector.inspect("{\"query\":\"public\"}", Set.of(protectedJson), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
    }
    @Test void freeTextFifthLayerCannotBeDeclaredSafeWithoutFindingTheSecret() {
        for (int count : List.of(5, 9)) {
            assertThat(inspector.inspect("Contexto: " + encode("harmless", count), Set.of(SECRET), 65536).state())
                    .isEqualTo(ProtectedContentInspector.State.UNINSPECTABLE);
        }
        assertThat(inspector.inspect("{{chatId}}", Set.of(SECRET), 65536).state()).isEqualTo(ProtectedContentInspector.State.SAFE);
        assertThat(inspector.inspect("{{chatId}}", Set.of("{{chatId}}"), 65536).state()).isEqualTo(ProtectedContentInspector.State.MATCH);
    }
}
