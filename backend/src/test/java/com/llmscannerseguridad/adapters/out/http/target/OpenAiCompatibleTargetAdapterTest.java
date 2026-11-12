package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.system.CapturedOutput;
import com.sun.net.httpserver.HttpServer;
import com.llmscannerseguridad.adapters.out.crypto.target.AesGcmCredentialEncryptionAdapter;
import com.llmscannerseguridad.adapters.out.execution.securitytest.*;
import com.llmscannerseguridad.adapters.out.validation.target.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.target.*;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiCompatibleTargetAdapterTest {
    static final String TOKEN = "synthetic-chat-token";
    private final OpenAiCompatibleCodec codec = new OpenAiCompatibleCodec();
    static Stream<Arguments> invalidBodies() {
        return Stream.of(
            Arguments.of("", ExecutionError.RESPONSE_INVALID_JSON),
            Arguments.of("{", ExecutionError.RESPONSE_INVALID_JSON),
            Arguments.of("{}", ExecutionError.RESPONSE_PATH_NOT_FOUND),
            Arguments.of("{\"choices\":[]}", ExecutionError.RESPONSE_PATH_NOT_FOUND),
            Arguments.of("{\"choices\":{}}", ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[{}]}", ExecutionError.RESPONSE_PATH_NOT_FOUND),
            Arguments.of("{\"choices\":[{\"message\":null}]}", ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[{\"message\":{}}]}", ExecutionError.RESPONSE_PATH_NOT_FOUND),
            Arguments.of(reply("null", "stop"), ExecutionError.RESPONSE_NOT_TEXT),
            Arguments.of(reply("42", "stop"), ExecutionError.RESPONSE_NOT_TEXT),
            Arguments.of(reply("{}", "stop"), ExecutionError.RESPONSE_NOT_TEXT),
            Arguments.of(reply("[]", "stop"), ExecutionError.RESPONSE_NOT_TEXT),
            Arguments.of(reply("\"partial\"", "length"), ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[{\"message\":{\"content\":\"x\"}}]}", ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"x\",\"tool_calls\":[{}]}}]}", ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[{},{}]}", ExecutionError.RESPONSE_FORMAT_ERROR),
            Arguments.of("{\"choices\":[],\"choices\":[]}", ExecutionError.RESPONSE_INVALID_JSON));
    }
    static String reply(String content, String finish) { return "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":"+content+"},\"finish_reason\":\""+finish+"\"}]}"; }
    @ParameterizedTest @MethodSource("invalidBodies")
    void strictErrors(String body, ExecutionError error) {
        assertThatThrownBy(()->codec.extract(body)).isInstanceOfSatisfying(TargetJsonProcessor.JsonFailure.class,
                failure->assertThat(failure.reason()).isEqualTo(error));
    }
    @ParameterizedTest @ValueSource(strings={"\"\"","\"  \"","\"😀\\u0000\""})
    void acceptsExactEmptyWhitespaceAndUnicode(String content) {
        assertThat(codec.extract(reply(content,"stop"))).isEqualTo(new StrictTargetJson().read(content));
    }
    @Test void exactRequestsBearerAndRedactionWithOptionalSystem(CapturedOutput output) throws Exception {
        try (var fixture = new Fixture()) {
            for (String system : new String[]{null," synthetic \"system\"\n"}) {
                var target=fixture.target(system);
                fixture.response.set(reply("\""+TOKEN+"\"","stop"));
                var interaction=fixture.adapter.interact(target,"prompt \"😀\"\n",budget());
                var request=(Map<?,?>)new StrictTargetJson().read(fixture.request.get());
                assertThat(request.keySet().stream().map(Object::toString).toList()).containsExactly("model","messages","stream");
                assertThat(request.get("model")).isEqualTo("fixture-model");assertThat(request.get("stream")).isEqualTo(false);
                var messages=(List<?>)request.get("messages");assertThat(messages).hasSize(system==null?1:2);
                assertThat(messages.getLast()).isEqualTo(Map.of("role","user","content","prompt \"😀\"\n"));
                if(system!=null) assertThat(messages.getFirst()).isEqualTo(Map.of("role","system","content",system));
                assertThat(fixture.authorization.get()).isEqualTo("Bearer "+TOKEN);
                assertThat(interaction.errorReason()).isNull();assertThat(interaction.responseText()).isEqualTo("[REDACTED_CREDENTIAL]");
                assertThat(interaction.responseRedacted()).isTrue();assertThat(interaction.legacy().requestBody()).doesNotContain(TOKEN);
                assertThat(target.settings().requestTemplate()).isNull();assertThat(target.settings().responsePath()).isNull();
            }
            assertThat(fixture.calls).hasValue(2);
            assertThat(output.getAll()).doesNotContain(TOKEN);
        }
    }
    @ParameterizedTest @ValueSource(ints={401,403,404,429,500,503,302})
    void httpErrorsNeverRetryOrCopyBody(int status) throws Exception {
        try(var f=new Fixture()) {
            f.status.set(status);f.response.set(TOKEN);
            var result=f.adapter.interact(f.target(null),"prompt",budget());
            assertThat(result.errorReason()).isEqualTo(ExecutionError.HTTP_STATUS_ERROR);
            assertThat(result.legacy().httpStatus()).isEqualTo(status);assertThat(result.responseText()).isNull();
            assertThat(f.calls).hasValue(1);assertThat(result.toString()).doesNotContain(TOKEN);
        }
    }
    @Test void timeoutAndCallbackFailureCloseResources() throws Exception {
        try(var f=new Fixture()) {
            f.delay.set(1600);
            assertThat(f.adapter.interact(f.target(null),"prompt",budget()).errorReason()).isEqualTo(ExecutionError.TARGET_TIMEOUT);
            f.delay.set(0);f.response.set("{invalid");
            assertThat(f.adapter.interact(f.target(null),"prompt",budget()).errorReason()).isEqualTo(ExecutionError.RESPONSE_INVALID_JSON);
            f.response.set(reply("\"ok\"","stop"));
            assertThat(f.adapter.interact(f.target(null),"prompt",budget()).responseText()).isEqualTo("ok");
        }
    }
    @Test void blockedDestinationNeverDecryptsAndRoutingIsExplicit() {
        var crypto=mock(com.llmscannerseguridad.application.target.CredentialDecryptionPort.class);
        var settings=new TargetSettings(null,new TargetUrl("https://controlled.invalid/chat"),null,null,Map.of(),AuthType.BEARER_TOKEN,null,1000,TargetAdapterType.OPENAI_COMPATIBLE,"m",null);
        var target=new TargetConfiguration(UUID.randomUUID(),UUID.randomUUID(),settings,new EncryptedCredential(new byte[]{1}),Instant.EPOCH,Instant.EPOCH);
        try(var transport=new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(),false,false,Set.of()),null,new TargetJsonProcessor(null),crypto,ExecutionLimits.defaults())) {
            assertThat(new OpenAiCompatibleTargetAdapter(transport).interact(target,"p",budget()).errorReason()).isEqualTo(ExecutionError.TARGET_NOT_AUTHORIZED);
            verifyNoInteractions(crypto);
        }
        var legacy=mock(TargetAdapter.class);var chat=mock(TargetAdapter.class);var router=new RoutingTargetAdapter(legacy,chat);
        var b=budget();router.interact(target,"p",b);verify(chat).interact(same(target),eq("p"),same(b));verifyNoInteractions(legacy);
        var rest=new TargetConfiguration(target.id(),target.projectId(),new TargetSettings(null,settings.url(),"{\"m\":\"{{PROMPT}}\"}","$.text",Map.of(),AuthType.NONE,null,1000),null,Instant.EPOCH,Instant.EPOCH);
        router.interact(rest,"p",b);verify(legacy).interact(same(rest),eq("p"),same(b));
    }
    private static ScanBudget budget() { return new ScanBudget(Instant.now(),300000,Clock.systemUTC(),System::nanoTime); }
    static class Fixture implements AutoCloseable {
        final HttpServer server;final AtomicReference<String> response=new AtomicReference<>(reply("\"ok\"","stop"));
        final AtomicReference<String> request=new AtomicReference<>(),authorization=new AtomicReference<>();
        final AtomicInteger calls=new AtomicInteger(),status=new AtomicInteger(200),delay=new AtomicInteger();
        final AesGcmCredentialEncryptionAdapter crypto=new AesGcmCredentialEncryptionAdapter(()->Base64.getEncoder().encodeToString(new byte[32]));
        final BoundedDnsResolver dns=new BoundedDnsResolver(InetAddress::getAllByName);
        final HttpTargetInvocationAdapter transport;final OpenAiCompatibleTargetAdapter adapter;
        Fixture() throws Exception {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange->{
                calls.incrementAndGet();request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                try { Thread.sleep(delay.get()); } catch(InterruptedException error){Thread.currentThread().interrupt();}
                byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");
                try {exchange.sendResponseHeaders(status.get(),bytes.length);exchange.getResponseBody().write(bytes);} finally {exchange.close();}
            });server.start();
            transport=new HttpTargetInvocationAdapter(new TargetDestinationPolicy(Set.of(),true,true,Set.of(origin())),dns,new TargetJsonProcessor(null),crypto,ExecutionLimits.defaults());
            adapter=new OpenAiCompatibleTargetAdapter(transport);
        }
        String origin(){return "http://127.0.0.1:"+server.getAddress().getPort();}
        TargetConfiguration target(String system){var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);return new TargetConfiguration(UUID.randomUUID(),UUID.randomUUID(),new TargetSettings(null,new TargetUrl(origin()+"/chat"),null,null,Map.of(),AuthType.BEARER_TOKEN,null,1000,TargetAdapterType.OPENAI_COMPATIBLE,"fixture-model",system),crypto.encrypt(TOKEN),now,now);}
        public void close(){transport.close();dns.close();server.stop(0);}
    }
}
