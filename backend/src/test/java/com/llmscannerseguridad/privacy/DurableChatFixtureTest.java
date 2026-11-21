package com.llmscannerseguridad.privacy;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.llmscannerseguridad.adapters.out.http.target.OpenAiCompatibleCodec;
import com.llmscannerseguridad.adapters.out.validation.target.TargetJsonProcessor;
import com.llmscannerseguridad.application.execution.InvocationResult;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class DurableChatFixtureTest {
    private static final String SECRET = "fixture-durable-private-91";
    private final OpenAiCompatibleCodec codec = new OpenAiCompatibleCodec();

    @Test void originalChatFixtureFailsProtocolBeforePrivacyInspectionForEveryEncoding() {
        for (String text : responses()) {
            String original = JsonMapper.shared().writeValueAsString(Map.of("choices",
                    List.of(Map.of("message", Map.of("content", text)))));
            assertThatThrownBy(() -> codec.extract(original))
                    .isInstanceOfSatisfying(TargetJsonProcessor.JsonFailure.class,
                            error -> assertThat(error.reason()).isEqualTo(ExecutionError.RESPONSE_FORMAT_ERROR));
        }
    }

    @Test void explicitStopPreservesAllN4PayloadsAndAllowsDurableRedaction() {
        for (String text : responses()) {
            String valid = JsonMapper.shared().writeValueAsString(Map.of("choices",
                    List.of(Map.of("message", Map.of("content", text), "finish_reason", "stop"))));
            assertThat(codec.extract(valid)).isEqualTo(text);
            try (var scope = ProtectionScope.open(UUID.randomUUID(), 524288)) {
                ProtectionScope.register(SECRET);
                assertThat(new ProtectedContentInspector().inspect(text, ProtectionScope.values(), 65536).state())
                        .isEqualTo(ProtectedContentInspector.State.MATCH);
                var safe = new DurableEvidenceProtection().protect(
                        new InvocationResult(200, 1L, null, "{}", codec.extract(valid), false, false), null);
                assertThat(safe.errorReason()).isNull();
                assertThat(safe.responseRedacted()).isTrue();
                assertThat(safe.responseText()).contains(ProtectedContentInspector.MARKER).doesNotContain(SECRET);
                assertThat(new ProtectedContentInspector().inspect(safe.responseText(), ProtectionScope.values(), 65536).state())
                        .isEqualTo(ProtectedContentInspector.State.SAFE);
            }
        }
    }

    private static List<String> responses() {
        return List.of(Character.toString(92) + "u0066" + SECRET.substring(1),
                ProtectedContentInspectorTest.encode(SECRET, 1), ProtectedContentInspectorTest.encode(SECRET, 2),
                ProtectedContentInspectorTest.encode(SECRET, 4));
    }
}
