package com.llmscannerseguridad.application.execution;

import java.util.Arrays;
import java.util.stream.Stream;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;

class LegacyInvocationResultProjectionTest {
    static Stream<InvocationResult> results() {
        return Stream.concat(Stream.of((ExecutionError) null), Arrays.stream(ExecutionError.values()))
                .flatMap(error -> Stream.of(0, 1, 2, 3).map(flags -> new InvocationResult(
                        error == null ? 200 : 503, 17L, error, "REQUEST \"\\\n\0😀e\u0301", "RESPONSE \n\0😀e\u0301",
                        (flags & 1) != 0, (flags & 2) != 0)));
    }

    static Stream<InvocationResult> nullableResults() {
        return Stream.of((String) null, "", " \0😀 ").flatMap(request ->
                Stream.of((String) null, "", " \0😀 ").flatMap(response ->
                Stream.of((Long) null, 0L, 19L).flatMap(latency ->
                Stream.of(0, 1, 2, 3).map(flags -> new InvocationResult(null, latency,
                        ExecutionError.HTTP_STATUS_ERROR, request, response, (flags & 1) != 0, (flags & 2) != 0)))));
    }

    @ParameterizedTest
    @MethodSource({"results", "nullableResults"})
    void allSevenFieldsRoundTripWithoutValidationOrDefaults(InvocationResult original) {
        var interaction = LegacyInvocationResultProjection.fromLegacy(original);
        assertThat(interaction.responseText()).isEqualTo(original.responseText());
        assertThat(interaction.latencyMs()).isEqualTo(original.latencyMs());
        assertThat(interaction.errorReason()).isSameAs(original.errorReason());
        if (original.errorReason() != null) {
            assertThat(interaction.errorReason().global()).isEqualTo(original.errorReason().global());
        }
        assertThat(interaction.requestRedacted()).isEqualTo(original.requestRedacted());
        assertThat(interaction.responseRedacted()).isEqualTo(original.responseRedacted());
        assertThat(interaction.legacy()).isNotNull();
        assertThat(interaction.legacy().httpStatus()).isEqualTo(original.httpStatus());
        assertThat(interaction.legacy().requestBody()).isEqualTo(original.requestBody());
        assertThat(LegacyInvocationResultProjection.toLegacy(interaction)).isEqualTo(original);
    }

    @Test
    void anomalousNullResultIsNotReclassifiedAndMissingLegacyIsNotDefaulted() {
        assertThat(LegacyInvocationResultProjection.fromLegacy(null)).isNull();
        assertThat(LegacyInvocationResultProjection.toLegacy(null)).isNull();
        assertThatThrownBy(() -> LegacyInvocationResultProjection.toLegacy(
                new NormalizedInteraction(null, null, null, false, false, null)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void diagnosticStringsNeverExposeBodiesOrResponse() {
        var interaction = LegacyInvocationResultProjection.fromLegacy(
                new InvocationResult(200, 0L, null, "PRIVATE_REQUEST_MARKER", "PRIVATE_RESPONSE_MARKER", true, true));
        assertThat(interaction.toString()).isEqualTo("NormalizedInteraction[errorReason=null]")
                .doesNotContain("PRIVATE_REQUEST_MARKER", "PRIVATE_RESPONSE_MARKER");
        assertThat(interaction.legacy().toString()).isEqualTo("LegacyCompatibility")
                .doesNotContain("PRIVATE_REQUEST_MARKER");
    }
}
