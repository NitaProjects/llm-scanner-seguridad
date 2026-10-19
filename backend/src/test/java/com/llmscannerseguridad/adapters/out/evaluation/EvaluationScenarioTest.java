package com.llmscannerseguridad.adapters.out.evaluation;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.EvaluationScenarioPort;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.execution.*;
import com.llmscannerseguridad.domain.securitytest.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class EvaluationScenarioTest {
    @TempDir Path directory;
    private final Instant now=Instant.parse("2026-10-10T10:00:00Z");
    private final UUID project=UUID.randomUUID(),target=UUID.randomUUID();
    private List<SecurityTest> tests() {
        var codes=List.of("LLM01-INSTRUCTION-001","LLM02-CANARY-001","LLM08-CONTEXT-001","LLM10-MARKUP-001");
        return codes.stream().map(code->new SecurityTest(UUID.randomUUID(),code,1,"Prueba",SecurityCategory.valueOf(code.substring(0,5)),"Prompt público")).toList();
    }
    private LocalEvaluationScenarioAdapter.Manifest manifest(List<SecurityTest> tests,List<String> corpus) {
        var entries=tests.stream().map(test->new EvaluationScenarioPort.Entry(test.code(),1,EvaluationFixtures.context(test.category().name()),corpus)).toList();
        return new LocalEvaluationScenarioAdapter.Manifest(1,List.of(new LocalEvaluationScenarioAdapter.ManifestScenario("synthetic-demo","1",project,target,
                now.toString(),entries,new LocalEvaluationScenarioAdapter.ExportPolicy(true,"OPENAI"))));
    }
    private List<com.llmscannerseguridad.application.evaluation.EvaluationPlan.Item> items(List<SecurityTest> tests) {
        return tests.stream().map(test->new com.llmscannerseguridad.application.evaluation.EvaluationPlan.Item(
                UUID.randomUUID(),test.id(),test.code(),test.revision(),test.category())).toList();
    }
    @Test void oneSnapshotBindsProjectTargetTimestampAndNeverReloadsChangedCorpus() throws Exception {
        var tests=tests();var path=directory.resolve("scenario.json");Files.writeString(path,EvaluationJson.write(manifest(tests,List.of("SAFE"))));
        var adapter=new LocalEvaluationScenarioAdapter(path,true,"OPENAI",Set.of("synthetic-demo"));
        var snapshot=adapter.snapshot(project,target,now,items(tests));
        assertThat(snapshot.exportAuthorized()).isTrue();assertThat(snapshot.manifestHash()).hasSize(64);
        Files.writeString(path,EvaluationJson.write(manifest(tests,List.of("CHANGED"))));
        assertThat(snapshot.entries().get(tests.getFirst().code()).exportableResponses()).containsExactly("SAFE");
        assertThat(adapter.snapshot(UUID.randomUUID(),target,now,items(tests))).isNull();
        assertThat(adapter.snapshot(project,UUID.randomUUID(),now,items(tests))).isNull();
        assertThat(adapter.snapshot(project,target,now.plusSeconds(1),items(tests))).isNull();
        assertThat(new LocalEvaluationScenarioAdapter(null,true,"OPENAI",Set.of()).snapshot(project,target,now,items(tests))).isNull();
    }
    @Test void manifestTrueStillRequiresGlobalPermissionAllowlistAndMatchingProvider() throws Exception {
        var tests=tests();var path=directory.resolve("scenario.json");
        Files.writeString(path,EvaluationJson.write(manifest(tests,List.of("SAFE"))));
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        var test=tests.get(1);var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        for(var adapter:List.of(new LocalEvaluationScenarioAdapter(path,false,"OPENAI",Set.of("synthetic-demo")),
                new LocalEvaluationScenarioAdapter(path,true,"OPENAI",Set.of()),
                new LocalEvaluationScenarioAdapter(path,true,"OPENAI",Set.of("another-scenario")),
                new LocalEvaluationScenarioAdapter(path,true,"OTHER",Set.of("synthetic-demo")))) {
            var snapshot=adapter.snapshot(project,target,now,items(tests));
            assertThat(snapshot).isNotNull();assertThat(snapshot.exportAuthorized()).isFalse();
            UUID id=UUID.randomUUID();
            var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,
                    projector.configuration(id,criteria,snapshot));
            var prepared=projector.prepare(pending,execution(pending,test,"SAFE"),test);
            assertThat(prepared.insufficiency()).isEqualTo(OutcomeReason.EXPORT_NOT_AUTHORIZED);
            assertThat(prepared.input()).isNull();
        }
        var authorized=new LocalEvaluationScenarioAdapter(path,true,"OPENAI",Set.of("synthetic-demo"));
        assertThat(authorized.snapshot(project,target,now,items(tests)).exportAuthorized()).isTrue();
        assertThat(authorized.snapshot(project,target,now.plusNanos(1000),items(tests))).isNull();
        assertThat(new LocalEvaluationScenarioAdapter(null,true,"OPENAI",Set.of("synthetic-demo"))
                .snapshot(project,target,now,items(tests))).isNull();
        Files.writeString(path,"{}");
        assertThatThrownBy(()->authorized.snapshot(project,target,now,items(tests))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void malformedDuplicateUnknownUnicodeAndSizeCapsAreRejectedWithoutEcho() throws Exception {
        var tests=tests();var path=directory.resolve("scenario.json");var valid=EvaluationJson.write(manifest(tests,List.of("SAFE")));
        var adapter=new LocalEvaluationScenarioAdapter(path,true,"OPENAI",Set.of("synthetic-demo"));
        for(String text:List.of("not json",valid+"{}",valid.replace("\"schemaVersion\":1","\"schemaVersion\":1,\"schemaVersion\":1"),
                valid.substring(0,valid.length()-1)+",\"url\":\"PRIVATE_INVALID\"}",valid.replace("SAFE","\\ud800")," ".repeat(262145))) {
            Files.writeString(path,text);
            assertThatThrownBy(()->adapter.snapshot(project,target,now,items(tests))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageNotContaining("PRIVATE_INVALID");
        }
        Files.writeString(path,EvaluationJson.write(manifest(tests,Collections.nCopies(65,"SAFE"))));
        assertThatThrownBy(()->adapter.snapshot(project,target,now,items(tests))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void catalogueHasFourImmutableCriteriaAndUnknownRevisionIsNotDefaulted() {
        var catalog=new ResourceEvaluationCriteriaCatalog();
        assertThat(tests().stream().map(catalog::find)).hasSize(4);
        var test=tests().getFirst();
        assertThatThrownBy(()->catalog.find(new SecurityTest(test.id(),test.code(),2,test.name(),test.category(),test.prompt())))
                .isInstanceOf(IllegalStateException.class).hasMessage("CRITERIA_UNAVAILABLE");
    }
    @Test void projectionRequiresExactFullCorpusAndExternalPermissionAndKeepsOnlySafeInput() {
        var test=tests().get(1);var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        for(var sample:List.of(new Object[]{"SAFE",false,OutcomeReason.EXPORT_NOT_AUTHORIZED},
                new Object[]{"UNKNOWN_SECRET",true,OutcomeReason.EVIDENCE_NOT_EXPORTABLE},new Object[]{"SAFE",true,null})) {
            UUID id=UUID.randomUUID();
            var scenario=new EvaluationScenarioPort.Scenario("synthetic-demo","1","hash",(boolean)sample[1],"OPENAI",
                    Map.of(test.code(),new EvaluationScenarioPort.Entry(test.code(),1,EvaluationFixtures.context("LLM02"),List.of("SAFE"))));
            var config=projector.configuration(id,criteria,scenario);
            assertThat(config.configurationSnapshot()).doesNotContain("exportableResponses","UNKNOWN_SECRET");
            var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,config);
            var execution=execution(pending,test,(String)sample[0]);
            var prepared=projector.prepare(pending,execution,test);
            assertThat(prepared.insufficiency()).isEqualTo(sample[2]);
            if(sample[2]!=null) assertThat(prepared.result().evidence().projection()).isNull();
            else assertThat(prepared.result().evidence().projection()).doesNotContain("credential","headers","TargetConfiguration","requestBody");
        }
    }
    @Test void exactEvidenceCapInputCapEscapesAndNulRoundTripNeverTruncate() {
        var test=tests().get(1);var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        for(String text:List.of("x".repeat(16384),"x".repeat(16385),"\"".repeat(16384),"a\0😀")) {
            UUID id=UUID.randomUUID();
            var scenario=new EvaluationScenarioPort.Scenario("synthetic-demo","1","hash",true,"OPENAI",Map.of(test.code(),
                    new EvaluationScenarioPort.Entry(test.code(),1,EvaluationFixtures.context("LLM02"),List.of(text))));
            var config=projector.configuration(id,criteria,scenario);
            var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,config);
            var prepared=projector.prepare(pending,execution(pending,test,text),test);
            if(text.length()==16385||text.startsWith("\"")) {
                assertThat(prepared.insufficiency()).isEqualTo(OutcomeReason.EVIDENCE_TOO_LARGE);
                assertThat(prepared.result().evidence().complete()).isFalse();assertThat(prepared.input()).isNull();
            } else {
                assertThat(prepared.insufficiency()).isNull();assertThat(prepared.input().evidenceText()).isEqualTo(text);
                var projection=prepared.result().evidence().projection();
                assertThat(projection).doesNotContain("\0");assertThat(EvaluationJson.parse(projection)).isNotNull();
            }
        }
    }
    private TestExecution execution(EvaluationResult pending,SecurityTest test,String response) {
        return TestExecution.pending(pending.testExecutionId(),pending.scanId(),test.id(),target,now).start(now,now)
                .finish(now,200,1L,null,"{\"doNotExport\":\"SYNTHETIC_SECRET\"}",response,false,false);
    }
    @Test void relevantRedactionIsInconclusiveWhileIrrelevantRedactionKeepsStrongEvidence() {
        var test=tests().get(1);var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        for(String text:List.of("[REDACTED_CREDENTIAL]","SYNTHETIC_CANARY_7 [REDACTED_CREDENTIAL]")) {
            UUID id=UUID.randomUUID();var scenario=new EvaluationScenarioPort.Scenario("synthetic-demo","1","hash",true,"OPENAI",Map.of(test.code(),
                    new EvaluationScenarioPort.Entry(test.code(),1,EvaluationFixtures.context("LLM02"),List.of(text))));
            var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,projector.configuration(id,criteria,scenario));
            var execution=TestExecution.pending(pending.testExecutionId(),pending.scanId(),test.id(),target,now).start(now,now)
                    .finish(now,200,1L,null,"{}",text,false,true);
            var prepared=projector.prepare(pending,execution,test);boolean relevant=text.startsWith("[");
            assertThat(prepared.insufficiency()).isEqualTo(relevant?OutcomeReason.EVIDENCE_INCOMPLETE:null);
            if(relevant) assertThat(prepared.input()).isNull();else assertThat(prepared.result().evidence().redacted()).isTrue();
        }
    }
    @Test void oversizedSnapshotsAndTamperedConfigurationFailBeforeAnyLlmInput() {
        var test=tests().get(1);var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        var base=EvaluationFixtures.context("LLM02");
        var oversized=new SyntheticContext("x".repeat(40000),base.hiddenContext(),base.hiddenAnchors(),base.publicValues(),base.restriction(),base.actionMarker(),
                base.markerViolationConfirmed(),base.expectedSafeResponses(),base.outputContract(),false,null,Severity.MEDIUM);
        var scenario=new EvaluationScenarioPort.Scenario("synthetic-demo","1","hash",true,"OPENAI",Map.of(test.code(),new EvaluationScenarioPort.Entry(test.code(),1,oversized,List.of("SAFE"))));
        assertThatThrownBy(()->projector.configuration(UUID.randomUUID(),criteria,scenario)).isInstanceOf(IllegalArgumentException.class);
        UUID id=UUID.randomUUID();var config=projector.configuration(id,criteria,null);
        var invalid=new EvaluationResult.Configuration(config.criteriaId(),config.criteriaVersion(),null,null,config.engineVersion(),config.policyVersion(),
                config.confidenceVersion(),config.severityVersion(),config.promptVersion(),config.schemaVersion(),config.llmInputProfile(),
                config.configurationSnapshot(),"0".repeat(64),config.requestedProvider(),config.requestedModel(),config.samplingParameters());
        var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,invalid);
        assertThatThrownBy(()->projector.prepare(pending,execution(pending,test,"SAFE"),test)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void redactedStructureCannotProveTargetViolationWhileIntactLiteralsRemainIndependent() {
        var test=tests().getLast();var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var projector=new EvaluationEvidenceProjector(EvaluationFixtures.settings(),new EvaluatorMessages(),Clock.fixed(now,ZoneOffset.UTC));
        var ctx=new SyntheticContext(null,null,List.of(),List.of(),null,null,false,List.of(),
                new SyntheticContext.OutputContract(List.of("FORBIDDEN_LITERAL"),List.of("text"),List.of("text","note"),
                        Map.of("text",SyntheticContext.ValueType.STRING,"note",SyntheticContext.ValueType.STRING),true),false,null,Severity.LOW);
        for(String text:List.of("{\"[REDACTED_CREDENTIAL]\":\"ok\"}","[REDACTED_CREDENTIAL]",
                "{\"text\":\"[REDACTED_CREDENTIAL]\",\"note\":\"FORBIDDEN_LITERAL\"}")) {
            UUID id=UUID.randomUUID();var scenario=new EvaluationScenarioPort.Scenario("synthetic-demo","1","hash",true,"OPENAI",Map.of(test.code(),
                    new EvaluationScenarioPort.Entry(test.code(),1,ctx,List.of(text))));
            var pending=EvaluationResult.pending(id,UUID.randomUUID(),UUID.randomUUID(),test.id(),test.category(),now,projector.configuration(id,criteria,scenario));
            var execution=TestExecution.pending(pending.testExecutionId(),pending.scanId(),test.id(),target,now).start(now,now)
                    .finish(now,200,1L,null,"{}",text,false,true);
            var prepared=projector.prepare(pending,execution,test);
            boolean independent=text.contains("FORBIDDEN_LITERAL");
            if(independent) {
                assertThat(prepared.insufficiency()).isNull();
                assertThat(prepared.result().deterministicSignals().getFirst().strength()).isEqualTo(RuleSignal.Strength.STRONG_POSITIVE);
            } else {
                assertThat(prepared.insufficiency()).isEqualTo(OutcomeReason.EVIDENCE_INCOMPLETE);
                assertThat(prepared.result().evidence().complete()).isFalse();assertThat(prepared.input()).isNull();
                assertThat(prepared.result().deterministicSignals().getFirst().result()).isEqualTo(RuleSignal.Result.NOT_APPLICABLE);
                assertThat(prepared.result().deterministicSignals()).noneSatisfy(s->assertThat(s.strength()).isEqualTo(RuleSignal.Strength.STRONG_POSITIVE));
                var decision=new HybridDecisionPolicy().consolidate(criteria,ctx,prepared.result().deterministicSignals(),prepared.insufficiency(),null,LlmStatus.SKIPPED,true);
                assertThat(decision.finalVerdict()).isEqualTo(Verdict.INCONCLUSIVE);assertThat(decision.confidence()).isEqualByComparingTo("0.00");
            }
        }
    }
}
