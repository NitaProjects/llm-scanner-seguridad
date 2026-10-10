package com.llmscannerseguridad.adapters.out.validation.target;

import java.util.Map;
import java.util.Set;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TargetJsonProcessorTest {
    private final TargetJsonProcessor json = new TargetJsonProcessor(
            new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of())));

    @Test
    void rendersWholeNodeWithoutExpandingPromptAndKeepsStructure() {
        String prompt = "comillas \" barras \\ salto\n 😀 {{PROMPT}}";
        String body = json.render(settings("{\"nested\":[{\"text\":\"{{PROMPT}}\"}],\"number\":12.125,\"flag\":true}"), prompt);
        assertThat(json.extract(body, "$.nested[0].text")).isEqualTo(prompt);
        assertThat(body).contains("\"number\":12.125", "\"flag\":true");
        for (String invalid : new String[] {"{\"m\":\"x{{PROMPT}}\"}", "{\"m\":\"{{PROMPT}}\",\"m\":0}", "{}"}) {
            assertThatThrownBy(() -> json.render(settings(invalid), prompt)).isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    void extractsOnlyOneStringIncludingRootEmptyAndQuotedProperties() {
        assertThat(json.extract("\"\"", "$")).isEmpty();
        assertThat(json.extract("{\"a.b\":[\"😀\\u0000\"]}", "$[\"a.b\"][0]")).isEqualTo("😀\0");
        failure("{\"a\":0}", "$.a", ExecutionError.RESPONSE_NOT_TEXT);
        failure("{\"a\":null}", "$.a", ExecutionError.RESPONSE_NOT_TEXT);
        failure("{}", "$.missing", ExecutionError.RESPONSE_PATH_NOT_FOUND);
        failure("[]", "$[0]", ExecutionError.RESPONSE_PATH_NOT_FOUND);
        for (String invalid : new String[] {"{}", "{\"a\":\"x\",\"a\":\"y\"}", "\"x\" \"y\"", "\"\\ud800\"", "[".repeat(65) + "0" + "]".repeat(65)}) {
            failure(invalid, "$.a", invalid.equals("{}") ? ExecutionError.RESPONSE_PATH_NOT_FOUND : ExecutionError.RESPONSE_INVALID_JSON);
        }
    }

    @Test
    void redactsDecodedStringsAndKeysAndFailsOnCollisionWithoutRawEvidence() {
        var evidence = json.redact("{\"s\\u0065cret\":\"secret\",\"other\":\"legitimate\"}", "Bearer secret 😀", "secret");
        assertThat(evidence.request()).doesNotContain("secret").contains("[REDACTED_CREDENTIAL]", "legitimate");
        assertThat(evidence.response()).isEqualTo("Bearer [REDACTED_CREDENTIAL] 😀");
        assertThat(evidence.requestRedacted()).isTrue();
        assertThat(evidence.responseRedacted()).isTrue();
        assertThat(evidence.toString()).doesNotContain("legitimate");
        assertThatThrownBy(() -> json.redact("{\"secret\":1,\"[REDACTED_CREDENTIAL]\":2}", "", "secret"))
                .isInstanceOf(TargetJsonProcessor.JsonFailure.class);
        assertThat(json.serializedTextBytes("\0")).isEqualTo(8);
    }

    private void failure(String document, String path, ExecutionError expected) {
        assertThatThrownBy(() -> json.extract(document, path)).isInstanceOfSatisfying(TargetJsonProcessor.JsonFailure.class,
                error -> assertThat(error.reason()).isEqualTo(expected));
    }

    private TargetSettings settings(String template) {
        return new TargetSettings(null, new TargetUrl("https://target.example/"), template, "$.a", Map.of(), AuthType.NONE, null, 10000);
    }
}
