package com.llmscannerseguridad.adapters.out.evaluation;

import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.*;
import org.junit.jupiter.api.Test;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.*;

class OpenAiTransportTest {
    @Test void realConnectionManagerSelectsTlsAndEmitsClientHelloWithoutExternalNetwork() throws Exception {
        var lookups = new AtomicInteger();
        var loopback = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        var resolver = new DnsResolver() {
            @Override public InetAddress[] resolve(String host) {
                assertThat(host).isEqualTo("tls-fixture.invalid");
                lookups.incrementAndGet();
                return new InetAddress[] {loopback};
            }
            @Override public String resolveCanonicalHostname(String host) { return host; }
        };
        // El peer solo identifica ClientHello y contesta datos inválidos: no acepta HTTP ni usa certificados.
        try (var peer = new ServerSocket(0, 1, loopback);
                var worker = Executors.newSingleThreadExecutor();
                var manager = OpenAiEvaluatorAdapter.connectionManager(resolver, 2000);
                var client = HttpClients.custom().setConnectionManager(manager)
                    .disableAutomaticRetries().disableRedirectHandling().build()) {
            peer.setSoTimeout(3000);
            var hello = worker.submit(() -> {
                try (var socket = peer.accept()) {
                    socket.setSoTimeout(3000);
                    var header = socket.getInputStream().readNBytes(5);
                    socket.getOutputStream().write("NOT_TLS\r\n".getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    return header;
                }
            });
            var request = new HttpGet("https://tls-fixture.invalid:" + peer.getLocalPort() + "/");
            assertThatThrownBy(() -> { try (var response = client.execute(request)) { } })
                    .isInstanceOf(javax.net.ssl.SSLException.class);
            var header = hello.get(4, TimeUnit.SECONDS);
            assertThat(header).hasSize(5);
            assertThat(header[0]).isEqualTo((byte) 22); // TLS handshake record.
            assertThat(header[1]).isEqualTo((byte) 3);
            assertThat(lookups).hasValue(1);
            assertThat(manager.getConnectionConfig().getConnectTimeout()).isEqualTo(Timeout.ofMilliseconds(2000));
            assertThat(manager.getTlsConfig().getHandshakeTimeout()).isEqualTo(Timeout.ofMilliseconds(2000));
        }
    }
    @Test void diagnosticsKeepFailureStagesAndExceptionTypesWithoutSensitiveDetails() {
        var logger = (Logger) LoggerFactory.getLogger(OpenAiEvaluatorAdapter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            String privateMessage = "SYNTHETIC_PRIVATE_ERROR Authorization: Bearer fixture-private-key";
            var errors = List.of(new java.net.UnknownHostException(privateMessage),
                    new javax.net.ssl.SSLHandshakeException(privateMessage), new java.net.ConnectException(privateMessage),
                    new SocketTimeoutException(privateMessage), new NullPointerException(privateMessage));
            var stages = List.of("DNS", "TLS_CONNECTION", "TLS_CONNECTION", "TIMEOUT", "INTERNAL");
            for (int i = 0; i < errors.size(); i++) {
                var error = errors.get(i);
                var time = new AtomicLong();
                try (var adapter = adapter((body, budget) -> { throw error; }, time)) {
                    assertThat(adapter.evaluate(input, budget(time)).errorReason())
                            .isEqualTo(error instanceof SocketTimeoutException ? LlmError.EVALUATOR_TIMEOUT : LlmError.EVALUATOR_UNAVAILABLE);
                    assertThat(appender.list.getLast().getFormattedMessage())
                            .contains("evaluatorFailure=" + stages.get(i), "exceptionType=" + error.getClass().getSimpleName());
                }
            }
            var time = new AtomicLong();
            try (var adapter = adapter((body, budget) -> new OpenAiEvaluatorAdapter.HttpReply(429, privateMessage), time)) {
                assertThat(adapter.evaluate(input, budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_RATE_LIMITED);
                assertThat(appender.list.getLast().getFormattedMessage()).contains("evaluatorFailure=HTTP_PROVIDER");
            }
            try (var adapter = adapter((body, budget) -> new OpenAiEvaluatorAdapter.HttpReply(200, privateMessage), time)) {
                assertThat(adapter.evaluate(input, budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_INVALID_OUTPUT);
                assertThat(appender.list.getLast().getFormattedMessage()).contains("evaluatorFailure=PARSE_SCHEMA");
            }
            assertThat(appender.list).hasSize(7).allSatisfy(event -> {
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getFormattedMessage()).doesNotContain("SYNTHETIC_PRIVATE_ERROR", "Authorization",
                        "fixture-private-key", "synthetic-fixture-key", "SYNTHETIC_CANARY_7");
                assertThat(java.util.Arrays.toString(event.getArgumentArray())).doesNotContain(privateMessage);
            });
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
    private final EvaluatorInput input=EvaluationFixtures.input(EvaluationFixtures.test(),"SYNTHETIC_CANARY_7");
    private OpenAiEvaluatorAdapter adapter(OpenAiEvaluatorAdapter.Transport transport,AtomicLong time) {
        return new OpenAiEvaluatorAdapter(EvaluationFixtures.settings(),new EvaluatorMessages(),()->"synthetic-fixture-key",transport,
                new EvaluatorInvocationGate(1000,time::get,time::addAndGet));
    }
    private EvaluationCallBudget budget(AtomicLong time) { return new EvaluationCallBudget(()->20000000000L,20000,time::get); }
    @Test void localContractResponsesAndAllStatusFailuresUseOneRequestAndNoRetries() {
        for(int status:List.of(200,429,401,403,400,402,404,500,503)) {
            var time=new AtomicLong();var calls=new AtomicInteger();
            try(var adapter=adapter((body,budget)->{calls.incrementAndGet();assertThat(body).doesNotContain("synthetic-fixture-key");
                return new OpenAiEvaluatorAdapter.HttpReply(status,EvaluationFixtures.response(input,Verdict.VULNERABLE));},time)) {
                var outcome=adapter.evaluate(input,budget(time));assertThat(calls).hasValue(1);
                assertThat(outcome.dispatched()).isTrue();
                assertThat(outcome.errorReason()).isEqualTo(status==200?null:OpenAiEvaluatorAdapter.classify(status));
            }
        }
    }
    @Test void missingOrBlankKeyNeverReachesTransportEvenWithPreparedInput() {
        for(String absent:java.util.Arrays.asList(null,""," ")) {
            var time=new AtomicLong();var calls=new AtomicInteger();
            try(var adapter=new OpenAiEvaluatorAdapter(EvaluationFixtures.settings(),new EvaluatorMessages(),()->absent,
                    (body,budget)->{calls.incrementAndGet();throw new AssertionError("El transporte debe permanecer bloqueado");},
                    new EvaluatorInvocationGate(1000,time::get,time::addAndGet))) {
                var outcome = adapter.evaluate(input,budget(time));
                assertThat(outcome.errorReason()).isEqualTo(LlmError.EVALUATOR_CONFIGURATION_INVALID);
                assertThat(outcome.dispatched()).isFalse();
                assertThat(calls).hasValue(0);
            }
        }
    }
    @Test void expiredBudgetAtInvocationGateNeverDispatchesPreparedBody() {
        var time = new AtomicLong();
        var calls = new AtomicInteger();
        try (var adapter = adapter((body, budget) -> {
            calls.incrementAndGet();
            throw new AssertionError("El presupuesto agotado no permite enviar el cuerpo");
        }, time)) {
            var outcome = adapter.evaluate(input, new EvaluationCallBudget(() -> 0L, 20000, time::get));
            assertThat(outcome.errorReason()).isEqualTo(LlmError.EVALUATOR_TIMEOUT);
            assertThat(outcome.dispatched()).isFalse();
            assertThat(calls).hasValue(0);
        }
    }
    @Test void timeoutConnectionDnsInvalidUtf8AndLateResponsesHaveClosedErrors() {
        var time=new AtomicLong();
        try(var adapter=adapter((body,budget)->{throw new SocketTimeoutException("SYNTHETIC_PRIVATE_ERROR");},time)) {
            assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_TIMEOUT);
        }
        try(var adapter=adapter((body,budget)->{throw new java.net.UnknownHostException("SYNTHETIC_PRIVATE_ERROR");},time)) {
            assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_UNAVAILABLE);
        }
        try(var adapter=adapter((body,budget)->{throw new java.net.ConnectException("SYNTHETIC_PRIVATE_ERROR");},time)) {
            assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_UNAVAILABLE);
        }
        try(var adapter=adapter((body,budget)->{time.addAndGet(20000000001L);return new OpenAiEvaluatorAdapter.HttpReply(200,EvaluationFixtures.response(input,Verdict.VULNERABLE));},time)) {
            assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_TIMEOUT);
        }
        try(var adapter=adapter((body,budget)->{time.addAndGet(20000000001L);throw new java.net.UnknownHostException("SYNTHETIC_PRIVATE_ERROR");},time)) {
            assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(LlmError.EVALUATOR_TIMEOUT);
        }
        assertThatThrownBy(()->EvaluationJson.read(new java.io.ByteArrayInputStream(new byte[]{(byte)0xc3,0x28}),16384)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void exactResponseCapIsAcceptedAndExcessIsNeverParsedAsSuccess() {
        String response=EvaluationFixtures.response(input,Verdict.VULNERABLE);
        for(int bytes:List.of(16384,16385)) {
            var time=new AtomicLong();String body=response+" ".repeat(bytes-EvaluationJson.bytes(response));
            try(var adapter=adapter((request,budget)->new OpenAiEvaluatorAdapter.HttpReply(200,body),time)) {
                assertThat(adapter.evaluate(input,budget(time)).errorReason()).isEqualTo(bytes==16384?null:LlmError.EVALUATOR_INVALID_OUTPUT);
            }
        }
    }
    @Test void sharedGateAllowsOnlyOneConcurrentInvocationAcrossScans() throws Exception {
        var time=new AtomicLong();var active=new AtomicInteger();var max=new AtomicInteger();var calls=new AtomicInteger();
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        try(var adapter=adapter((request,budget)->{
            int concurrent=active.incrementAndGet();max.accumulateAndGet(concurrent,Math::max);
            try {if(calls.incrementAndGet()==1){entered.countDown();assertThat(release.await(5,TimeUnit.SECONDS)).isTrue();}
                return new OpenAiEvaluatorAdapter.HttpReply(200,EvaluationFixtures.response(input,Verdict.VULNERABLE));
            } finally {active.decrementAndGet();}
        },time);var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->adapter.evaluate(input,budget(time)));assertThat(entered.await(5,TimeUnit.SECONDS)).isTrue();
            var second=pool.submit(()->adapter.evaluate(input,budget(time)));release.countDown();
            assertThat(first.get(5,TimeUnit.SECONDS).errorReason()).isNull();assertThat(second.get(5,TimeUnit.SECONDS).errorReason()).isNull();
            assertThat(max).hasValue(1);assertThat(time.get()).isGreaterThanOrEqualTo(1000000000L);
        }
    }
}
