package com.llmscannerseguridad.adapters.out.validation.target;

import java.util.Set;
import com.llmscannerseguridad.domain.target.TargetUrl;
import com.llmscannerseguridad.domain.target.TargetValidationException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TargetValidationTest {
    private final JsonTargetConfigurationValidator validator =
            new JsonTargetConfigurationValidator(new LocalTargetUrlPolicy(false, false, Set.of()));

    @Test
    void urlSyntaxRestrictedLiteralsAndExplicitLocalDemoWithoutNetwork() {
        var policy = new LocalTargetUrlPolicy(false, false, Set.of());
        policy.validate(new TargetUrl(" HTTPS://EXAMPLE.test:443/chat "));
        for (String value : new String[] {"/chat", "ftp://example.test", "http://user:pass@example.test",
                "http://example.test/#x", "http://example.test:0", "http://example.test:65536",
                "http://example.test:", "http://exa mple.test", "http://example.test/\n",
                "http://localhost", "http://LOCALHOST.", "http://a.localhost.", "http://127.0.0.1",
                "http://10.0.0.1", "http://172.16.0.1", "http://192.168.1.1", "http://[::1]",
                "http://[fc00::1]", "http://[::ffff:127.0.0.1]", "http://169.254.169.254",
                "http://[fe80::1]"}) {
            assertThatThrownBy(() -> policy.validate(new TargetUrl(value)))
                    .isInstanceOf(TargetValidationException.class);
        }
        var local = new LocalTargetUrlPolicy(true, true, Set.of("http://localhost:8081"));
        local.validate(new TargetUrl("http://LOCALHOST.:8081/chat"));
        for (var inactive : new LocalTargetUrlPolicy[] {
                new LocalTargetUrlPolicy(false, true, Set.of("http://localhost:8081")),
                new LocalTargetUrlPolicy(true, false, Set.of("http://localhost:8081"))}) {
            assertThatThrownBy(() -> inactive.validate(new TargetUrl("http://localhost:8081")))
                    .isInstanceOf(TargetValidationException.class);
        }
        assertThatThrownBy(() -> local.validate(new TargetUrl("http://localhost:8082")))
                .isInstanceOf(TargetValidationException.class);
        assertThatThrownBy(() -> new LocalTargetUrlPolicy(true, true, Set.of("http://169.254.169.254")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalTargetUrlPolicy(true, true, Set.of("http://[fd00:ec2::254]")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalTargetUrlPolicy(true, true, Set.of("http://localhost:8081/only-path")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new TargetUrl("https://EXAMPLE.test/a").sameOrigin(new TargetUrl("https://example.test:443/b"))).isTrue();
        assertThat(new TargetUrl("http://[::1]/a").sameOrigin(new TargetUrl("http://[0:0:0:0:0:0:0:1]:80/b"))).isTrue();
        assertThat(new TargetUrl("http://[::ffff:127.0.0.1]").sameOrigin(new TargetUrl("http://127.0.0.1"))).isTrue();
        assertThat(new TargetUrl("http://example.test").sameOrigin(new TargetUrl("https://example.test"))).isFalse();
        String boundary = "https://example.test/" + "a".repeat(2048 - "https://example.test/".length());
        new TargetUrl(boundary);
        assertThatThrownBy(() -> new TargetUrl(boundary + "a")).isInstanceOf(TargetValidationException.class);
    }

    @Test
    void templateIsStrictObjectWithExactlyOneDecodedCompleteValueAndUtf8Limit() {
        validator.template(" { \"message\": \"{{PROMPT}}\", \"temperature\":0.2 } ");
        validator.template("{\"message\":\"\\u007b{PROMPT}}\"}");
        for (String value : new String[] {"", "{}", "[]", "null", "{", "{\"a\":\"x\"}",
                "{\"a\":\"{{PROMPT}}\",\"a\":\"{{PROMPT}}\"}",
                "{\"a\":\"{{PROMPT}}\",\"b\":\"{{PROMPT}}\"}",
                "{\"{{PROMPT}}\":\"value\"}", "{\"a\":\"prefix {{PROMPT}}\"}",
                "{\"a\":\"{{PROMPT}}\"} {}", "{\"a\":\"{{PROMPT}}\",\"b\":\"\\u0000\"}",
                "{\"\\u0000\":\"{{PROMPT}}\"}"}) {
            assertThatThrownBy(() -> validator.template(value)).isInstanceOf(TargetValidationException.class);
        }
        String start = "{\"a\":\"{{PROMPT}}\",\"b\":\"";
        String finish = "\"}";
        String exact = start + "x".repeat(32768 - start.length() - finish.length()) + finish;
        validator.template(exact);
        assertThatThrownBy(() -> validator.template(exact + " ")).isInstanceOf(TargetValidationException.class);
    }

    @Test
    void restrictedPathSupportsPropertiesIndicesRootAndRejectsComplexExpressions() {
        for (String value : new String[] {"$", "$.response.text", "$.choices[0].message.content",
                "$[\"response-text\"]", "$[\"espacio ñ\"][2147483647]", "$[\"quote\\\"name\"]",
                "$[\"\\u0061\"]", "$" + ".a".repeat(32)}) {
            validator.responsePath(value);
        }
        for (String value : new String[] {"", "response", "$.", "$..a", "$[*]", "$[?(@.x)]", "$[0,1]",
                "$[0:1]", "$[-1]", "$[01]", "$[+1]", "$[2147483648]", "$['a']", "$[\"\"]",
                "$[\"\\u0000\"]", "$[\"bad\\q\"]", "$[0]x", "$" + ".a".repeat(33),
                "$[9999999999999999999999999999999999999999]"}) {
            assertThatThrownBy(() -> validator.responsePath(value)).isInstanceOf(TargetValidationException.class);
        }
        String exact = "$[\"" + "a".repeat(1019) + "\"]";
        validator.responsePath(exact);
        assertThatThrownBy(() -> validator.responsePath(exact.replace("a\"]", "aa\"]")))
                .isInstanceOf(TargetValidationException.class);
    }
}
