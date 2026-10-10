package com.llmscannerseguridad.adapters.out.evaluation;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OpenAiTransportTest {
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
                assertThat(outcome.errorReason()).isEqualTo(status==200?null:OpenAiEvaluatorAdapter.classify(status));
            }
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
