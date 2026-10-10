package com.llmscannerseguridad.integration;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.llmscannerseguridad.adapters.in.rest.scan.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort;
import com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.config.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.reporting.SecurityReport;
import com.llmscannerseguridad.domain.scan.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static com.llmscannerseguridad.integration.ScoringDatabaseFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
@Timeout(90)
class ReportingPostgresIntegrationTest {
    private ConfigurableApplicationContext open(ScanTestDatabase db) { return db.openSecurity(Map.of("logging.level.root","ERROR")); }
    private TransactionTemplate tx(ConfigurableApplicationContext c) { return new TransactionTemplate(c.getBean(PlatformTransactionManager.class)); }
    private Ids reporting(ScanTestDatabase db,Verdict... vs) {
        var ids=seed(db,true,vs);db.jdbc().update("update scans set report_version='report-v1' where id=?",ids.scan());return ids;
    }
    private TransactionalScanLifecycle lifecycle(ConfigurableApplicationContext c,SecurityReportRepositoryPort reports,GenerateReportUseCase generator,ScanRepositoryPort scans,PlatformTransactionManager manager) {
        var delegate=new ScanLifecycleService(c.getBean(ProjectRepositoryPort.class),c.getBean(TargetConfigurationRepositoryPort.class),scans,Clock.fixed(TIME,ZoneOffset.UTC),UUID::randomUUID);
        return new TransactionalScanLifecycle(delegate,manager,c.getBean(ScanExecutionGate.class),c.getBean(TestExecutionRepositoryPort.class),c.getBean(EvaluationRepositoryPort.class),
                c.getBean(ScanScoreRepositoryPort.class),c.getBean(CalculateScanScoreUseCase.class),scans,reports,generator);
    }
    @Test void zeroInconclusiveMixedLiteralTextsVersionsAndIndependentRoundTrip() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var life=c.getBean(ScanLifecyclePort.class);var reports=c.getBean(GetSecurityReportUseCase.class);
            for(var vs:List.of(new Verdict[]{Verdict.NOT_VULNERABLE},new Verdict[]{Verdict.INCONCLUSIVE},new Verdict[]{Verdict.VULNERABLE,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE})) {
                var ids=reporting(db,vs);
                db.jdbc().update("update findings set title='Observable output issue',explanation='Controlled description',evidence='Synthetic evidence summary',recommendation='Review the consumer' where scan_id=?",ids.scan());
                db.jdbc().update("update evaluation_results set engine_version='engine-fixture-v1',criteria_id='criterion-fixture',criteria_version='1' where scan_id=?",ids.scan());
                var findings=db.jdbc().queryForList("select * from findings where scan_id=? order by id",ids.scan());
                var evaluations=db.jdbc().queryForList("select * from evaluation_results where scan_id=? order by id",ids.scan());
                db.jdbc().update("update target_configurations set url='https://changed-before-close.invalid',updated_at=updated_at+interval '1 second' where project_id=?",ids.project());
                assertThat(reports.execute(ids.project(),ids.scan()).availability()).isEqualTo(ReportAvailability.PENDING);
                assertThat(life.complete(ids.scan())).isTrue();var listing=reports.execute(ids.project(),ids.scan());var r=listing.report();
                assertThat(listing.availability()).isEqualTo(ReportAvailability.AVAILABLE);assertThat(r.summary().globalScore()).isEqualTo(vs.length==1?0:76);
                assertThat(r.scope().targetUpdatedAt()).isEqualTo(TIME);
                assertThat(r.summary().inconclusiveCount()).isEqualTo(vs[0]==Verdict.INCONCLUSIVE?4:vs.length==1?0:1);
                assertThat(r.evaluations()).allSatisfy(e->{assertThat(e.versions().engineVersion()).isEqualTo("engine-fixture-v1");assertThat(e.versions().promptVersion()).isNull();});
                assertThat(r.findings()).allSatisfy(f->{assertThat(f.title()).isEqualTo("Observable output issue");assertThat(f.description()).isEqualTo("Controlled description");assertThat(f.evidenceSummary()).isEqualTo("Synthetic evidence summary");assertThat(f.recommendation()).isEqualTo("Review the consumer");});
                assertThat(life.complete(ids.scan())).isTrue();assertThat(reports.execute(ids.project(),ids.scan())).isEqualTo(listing);
                assertThat(db.jdbc().queryForList("select * from findings where scan_id=? order by id",ids.scan())).isEqualTo(findings);
                assertThat(db.jdbc().queryForList("select * from evaluation_results where scan_id=? order by id",ids.scan())).isEqualTo(evaluations);
                db.jdbc().update("update projects set name='Later name' where id=?",ids.project());db.jdbc().update("update target_configurations set url='https://changed.invalid',updated_at=updated_at+interval '1 second' where project_id=?",ids.project());
                assertThat(reports.execute(ids.project(),ids.scan())).isEqualTo(listing);
                var mvc=MockMvcBuilders.standaloneSetup(c.getBean(ReportController.class)).setControllerAdvice(new EvaluationExceptionHandler()).build();
                mvc.perform(get("/api/projects/"+ids.project()+"/scans/"+ids.scan()+"/report")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                        .andExpect(jsonPath("$.report",org.hamcrest.Matchers.aMapWithSize(12))).andExpect(jsonPath("$.report.evaluations[0]",org.hamcrest.Matchers.aMapWithSize(19)))
                        .andExpect(jsonPath("$.report.generatedAt").value(new java.time.format.DateTimeFormatterBuilder().appendInstant(6).toFormatter().format(r.generatedAt())));
                var other=UUID.randomUUID();db.jdbc().update("insert into projects(id,name,created_at,updated_at) values(?,'Otro proyecto',?,?)",other,java.sql.Timestamp.from(TIME),java.sql.Timestamp.from(TIME));
                mvc.perform(get("/api/projects/"+other+"/scans/"+ids.scan()+"/report")).andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value("SCAN_NOT_FOUND")).andExpect(header().string("Cache-Control","no-store"));
            }
            var saved=db.jdbc().queryForList("select * from security_reports order by report_id");db.closeApplication(c);c=open(db);
            assertThat(db.jdbc().queryForList("select * from security_reports order by report_id")).isEqualTo(saved);
            assertThat(c.getBean(SecurityReportRepositoryPort.class)).isNotNull();
        }
    }
    @Test void concurrentCloseStableIdentityCandidatesUniqueAndNoOverwrite() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=reporting(db,Verdict.VULNERABLE);var life=c.getBean(ScanLifecyclePort.class);
            try(var pool=Executors.newFixedThreadPool(2)) {var start=new CountDownLatch(1);var a=pool.submit(()->{start.await();return life.complete(ids.scan());});var b=pool.submit(()->{start.await();return life.complete(ids.scan());});start.countDown();assertThat(a.get()).isTrue();assertThat(b.get()).isTrue();}
            var repo=c.getBean(SecurityReportRepositoryPort.class);var original=c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).report();
            var candidate=new SecurityReport(UUID.randomUUID(),original.scanId(),original.projectId(),original.generatedAt().plusSeconds(1),original.reportVersion(),original.locale(),original.scope(),original.summary(),original.evaluations(),original.findings(),original.interpretation(),original.limitations());
            SecurityReport reused=tx(c).execute(s->repo.insert(candidate));assertThat(reused).isEqualTo(original);
            var altered=new SecurityReport(candidate.reportId(),candidate.scanId(),UUID.randomUUID(),candidate.generatedAt(),candidate.reportVersion(),candidate.locale(),candidate.scope(),candidate.summary(),candidate.evaluations(),candidate.findings(),candidate.interpretation(),candidate.limitations());
            assertThatThrownBy(()->tx(c).execute(s->repo.insert(altered))).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->db.jdbc().execute("insert into security_reports select * from security_reports")).isInstanceOf(RuntimeException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isEqualTo(1);
            assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).report()).isEqualTo(original);
        }
    }
    @Test void failuresInConstructionSerializationValidationInsertAfterInsertAndTransitionRollBackTogether() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);
            for(String stage:List.of("construction","serialization","validation","insert","after-insert","transition")) {
                var ids=reporting(db,Verdict.VULNERABLE);var repo=spy(c.getBean(SecurityReportRepositoryPort.class));
                var generator=c.getBean(GenerateReportUseCase.class);var scans=c.getBean(ScanRepositoryPort.class);
                if(stage.equals("construction")) generator=new GenerateReportUseCase(id->{throw new IllegalStateException("SYNTHETIC_PRIVATE");},c.getBean(ReportPresentationPort.class),Clock.systemUTC(),UUID::randomUUID);
                else if(stage.equals("serialization")) doAnswer(call->{com.llmscannerseguridad.adapters.reporting.ReportJson.guard(262145,262144);return null;}).when(repo).insert(any());
                else if(stage.equals("validation")) generator=new GenerateReportUseCase(c.getBean(ReportInputPort.class),(v,l)->{throw new IllegalStateException("bad-resource");},Clock.systemUTC(),UUID::randomUUID);
                else if(stage.equals("insert")) doThrow(ScanOperationException.internal()).when(repo).insert(any());
                else if(stage.equals("after-insert")) doAnswer(call->{call.callRealMethod();assertThat(db.jdbc().queryForObject("select count(*) from security_reports where scan_id=?",Integer.class,ids.scan())).isZero();throw ScanOperationException.internal();}).when(repo).insert(any());
                else {scans=mock(ScanRepositoryPort.class,org.mockito.AdditionalAnswers.delegatesTo(scans));doReturn(false).when(scans).transition(eq(ScanStatus.RUNNING),any());}
                var failing=lifecycle(c,repo,generator,scans,c.getBean(PlatformTransactionManager.class));
                assertThatThrownBy(()->failing.complete(ids.scan())).as(stage).isInstanceOf(ScanOperationException.class);
                assertThat(db.jdbc().queryForObject("select count(*) from scan_scores where scan_id=?",Integer.class,ids.scan())).isZero();
                assertThat(db.jdbc().queryForObject("select count(*) from security_reports where scan_id=?",Integer.class,ids.scan())).isZero();
                assertThat(db.jdbc().queryForObject("select status from scans where id=?",String.class,ids.scan())).isEqualTo("RUNNING");
                assertThat(c.getBean(ScanLifecyclePort.class).fail(ids.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
                assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ReportAvailability.UNAVAILABLE);
                assertThat(db.jdbc().queryForObject("select count(*) from findings where scan_id=?",Integer.class,ids.scan())).isEqualTo(4);
            }
        }
    }
    @Test void pollingDuringUncommittedReportSeesPendingThenAvailable() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=reporting(db,Verdict.INCONCLUSIVE);var repo=spy(c.getBean(SecurityReportRepositoryPort.class));var inserted=new CountDownLatch(1);var release=new CountDownLatch(1);
            doAnswer(call->{var result=call.callRealMethod();inserted.countDown();if(!release.await(20,TimeUnit.SECONDS))throw ScanOperationException.internal();return result;}).when(repo).insert(any());
            var life=lifecycle(c,repo,c.getBean(GenerateReportUseCase.class),c.getBean(ScanRepositoryPort.class),c.getBean(PlatformTransactionManager.class));
            try(var pool=Executors.newSingleThreadExecutor()) {var task=pool.submit(()->life.complete(ids.scan()));
                try {assertThat(inserted.await(20,TimeUnit.SECONDS)).isTrue();for(int i=0;i<15;i++) assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ReportAvailability.PENDING);}
                finally {release.countDown();} assertThat(task.get()).isTrue();}
            assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ReportAvailability.AVAILABLE);
        }
    }
    @Test void ambiguousCommitPreservesConfirmedTrioOrAllowsConditionalCompensation() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);
            for(boolean committed:List.of(true,false)) {
                var ids=reporting(db,Verdict.VULNERABLE);var real=c.getBean(PlatformTransactionManager.class);var once=new java.util.concurrent.atomic.AtomicBoolean(true);
                PlatformTransactionManager ambiguous=new PlatformTransactionManager() {
                    @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return real.getTransaction(definition); }
                    @Override public void rollback(TransactionStatus s) { real.rollback(s); }
                    @Override public void commit(TransactionStatus s) {
                        if(once.getAndSet(false)) {if(committed)real.commit(s);else real.rollback(s);throw new IllegalStateException("SYNTHETIC_COMMIT_UNKNOWN");}real.commit(s);
                    }
                };
                var life=lifecycle(c,c.getBean(SecurityReportRepositoryPort.class),c.getBean(GenerateReportUseCase.class),c.getBean(ScanRepositoryPort.class),ambiguous);
                assertThatThrownBy(()->life.complete(ids.scan())).isInstanceOf(ScanOperationException.class);
                assertThat(life.fail(ids.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).isEqualTo(!committed);
                assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(committed?ReportAvailability.AVAILABLE:ReportAvailability.UNAVAILABLE);
                assertThat(db.jdbc().queryForObject("select count(*) from security_reports where scan_id=?",Integer.class,ids.scan())).isEqualTo(committed?1:0);
            }
        }
    }
    @Test void privateSourceDocumentsNeverEnterSnapshotAndRealWorkerProducesFourInconclusiveWithoutExternalServices() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=db.openSecurity(Map.of("logging.level.root","ERROR"),app -> app.registerBean("reportingFakeTarget",com.llmscannerseguridad.application.execution.TargetInvocationPort.class,
                    () -> (target,prompt,budget) -> new com.llmscannerseguridad.application.execution.InvocationResult(200,1L,null,"{}","SAFE",false,false),def -> def.setPrimary(true)));
            var ids=reporting(db,Verdict.VULNERABLE);
            db.jdbc().update("""
                    update evaluation_results set llm_status='SUCCEEDED',llm_attempt_count=1,llm_assessment='{"private":"SYNTHETIC_PRIVATE_ASSESSMENT"}',
                        deterministic_signals='[{"private":"SYNTHETIC_PRIVATE_SIGNALS"}]',configuration_snapshot='{"prompt":"SYNTHETIC_PRIVATE_PROMPT","key":"SYNTHETIC_PRIVATE_KEY"}',
                        evidence_projection='SYNTHETIC_PRIVATE_EVIDENCE',provider_request_id='SYNTHETIC_PRIVATE_PROVIDER'
                    where scan_id=?
                    """,ids.scan());
            db.jdbc().update("update test_executions set response_payload='\"SYNTHETIC_PRIVATE_RESPONSE\"',request_payload='{\"Authorization\":\"SYNTHETIC_PRIVATE_AUTH\"}' where scan_id=?",ids.scan());
            var life=c.getBean(ScanLifecyclePort.class);life.complete(ids.scan());var saved=c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).report();
            var json=com.llmscannerseguridad.adapters.reporting.ReportJson.write(saved);assertThat(json).doesNotContain("SYNTHETIC_PRIVATE","Authorization","llmAssessment","deterministicSignals","configurationSnapshot");
            var scan=life.create(ids.project());c.getBean(ScanWorkerEntry.class).execute(scan.id());
            var actual=c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),scan.id());assertThat(actual.availability()).isEqualTo(ReportAvailability.AVAILABLE);
            assertThat(actual.report().summary().globalScore()).isZero();assertThat(actual.report().summary().inconclusiveCount()).isEqualTo(4);
            assertThat(actual.report().findings()).isEmpty();assertThat(actual.report().evaluations()).allSatisfy(e->assertThat(e.llmStatus()).isEqualTo(LlmStatus.SKIPPED));
        }
    }
    @Test void newCreationMarkersRollbackAndWorkerCompletesWithoutOpenAi() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=seed(db,true,Verdict.NOT_VULNERABLE);c.getBean(ScanLifecyclePort.class).fail(ids.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED);
            var life=c.getBean(ScanLifecyclePort.class);var pending=life.create(ids.project());
            assertThat(db.jdbc().queryForMap("select report_version,scoring_version,evaluation_profile_version from scans where id=?",pending.id()))
                    .containsEntry("report_version","report-v1").containsEntry("scoring_version","scoring-v1").containsEntry("evaluation_profile_version","evaluation-engine-v1");
            life.fail(pending.id(),ScanStatus.PENDING,ScanFailureReason.SCAN_PROCESSING_FAILED);
            assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),pending.id()).availability()).isEqualTo(ReportAvailability.UNAVAILABLE);
            var repo=spy(c.getBean(SecurityReportRepositoryPort.class));doAnswer(call->{call.callRealMethod();throw ScanOperationException.internal();}).when(repo).setRequiredVersion(any());
            var broken=lifecycle(c,repo,c.getBean(GenerateReportUseCase.class),c.getBean(ScanRepositoryPort.class),c.getBean(PlatformTransactionManager.class));
            int before=db.jdbc().queryForObject("select count(*) from scans",Integer.class);
            assertThatThrownBy(()->broken.create(ids.project())).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from scans",Integer.class)).isEqualTo(before);
            var interrupted=reporting(db,Verdict.NOT_VULNERABLE);var generator=new GenerateReportUseCase(id->{throw ScanOperationException.internal();},c.getBean(ReportPresentationPort.class),Clock.systemUTC(),UUID::randomUUID);
            var failing=lifecycle(c,c.getBean(SecurityReportRepositoryPort.class),generator,c.getBean(ScanRepositoryPort.class),c.getBean(PlatformTransactionManager.class));
            // El mismo worker recibe un Scan reclamable; las fuentes ya confirmadas simulan el trabajo local.
            db.jdbc().update("update scans set status='PENDING',started_at=null where id=?",interrupted.scan());
            var failures=mock(ScanFailureHandlerPort.class);new ProcessScanUseCase(failing,id->{},failures).execute(interrupted.scan());
            assertThat(db.jdbc().queryForObject("select error_reason from scans where id=?",String.class,interrupted.scan())).isEqualTo("SCAN_PROCESSING_FAILED");verifyNoInteractions(failures);
        }
    }
    @Test void corruptedDurableStatesFailGetAndRecoveryAndKeepAdmissionClosed() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=reporting(db,Verdict.NOT_VULNERABLE);var life=c.getBean(ScanLifecyclePort.class);life.complete(ids.scan());
            var original=db.jdbc().queryForList("select * from security_reports");
            for(String mutation:List.of("status='RUNNING',finished_at=null","status='FAILED',error_reason='SCAN_PROCESSING_FAILED'","report_version=null")) {
                var old=db.jdbc().queryForMap("select status,finished_at,error_reason,report_version from scans where id=?",ids.scan());
                db.jdbc().execute("update scans set "+mutation+" where id='"+ids.scan()+"'");
                assertThatThrownBy(()->c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
                assertThatThrownBy(life::recoverInterrupted).isInstanceOf(ScanOperationException.class);
                var gate=new ScanExecutionGate();assertThatThrownBy(()->new ScanStartup(new RecoverInterruptedScansUseCase(life),gate).initialize()).isInstanceOf(RuntimeException.class);
                assertThatThrownBy(()->gate.admit(()->true)).isInstanceOf(ScanOperationException.class);
                db.jdbc().update("update scans set status=?,finished_at=?,error_reason=?,report_version=? where id=?",old.get("status"),old.get("finished_at"),old.get("error_reason"),old.get("report_version"),ids.scan());
            }
            db.jdbc().execute("alter table security_reports drop constraint ck_security_reports_payload");
            db.jdbc().execute("update security_reports set payload=payload || '{\"extra\":true}'::jsonb");
            assertThatThrownBy(()->c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(life::recoverInterrupted).isInstanceOf(RuntimeException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isEqualTo(original.size());
        }
    }
    @Test void failedCompensationClosesGateUntilRecoveryWithoutFabricatingResults() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=reporting(db,Verdict.VULNERABLE);
            db.jdbc().update("update scans set status='PENDING',started_at=null where id=?",ids.scan());
            var generator=new GenerateReportUseCase(id->{throw ScanOperationException.internal();},c.getBean(ReportPresentationPort.class),Clock.systemUTC(),UUID::randomUUID);
            var failing=spy(lifecycle(c,c.getBean(SecurityReportRepositoryPort.class),generator,c.getBean(ScanRepositoryPort.class),c.getBean(PlatformTransactionManager.class)));
            doThrow(ScanOperationException.internal()).when(failing).fail(any(),any(),any());var gate=c.getBean(ScanExecutionGate.class);
            new ProcessScanUseCase(failing,id->{},gate).execute(ids.scan());
            assertThatThrownBy(()->gate.admit(()->true)).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select status from scans where id=?",String.class,ids.scan())).isEqualTo("RUNNING");
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isZero();
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
            assertThat(db.jdbc().queryForObject("select count(*) from findings where scan_id=?",Integer.class,ids.scan())).isEqualTo(4);
            new ScanStartup(new RecoverInterruptedScansUseCase(c.getBean(ScanLifecyclePort.class)),gate).initialize();
            assertThat(gate.admit(()->true)).isTrue();assertThat(c.getBean(GetSecurityReportUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ReportAvailability.UNAVAILABLE);
        }
    }
    @Test void missingReportOrDivergentSummaryProjectAndUnknownVersionAreIntegrityErrors() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var ids=reporting(db,Verdict.VULNERABLE);var life=c.getBean(ScanLifecyclePort.class);life.complete(ids.scan());
            var get=c.getBean(GetSecurityReportUseCase.class);var original=db.jdbc().queryForObject("select payload::text from security_reports",String.class);
            for(String mutation:List.of("payload=jsonb_set(payload,'{summary,globalScore}','80')","payload=jsonb_set(payload,'{projectId}',to_jsonb('00000000-0000-0000-0000-000000000000'::text))")) {
                db.jdbc().execute("update security_reports set "+mutation);
                assertThatThrownBy(()->get.execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
                assertThatThrownBy(life::recoverInterrupted).isInstanceOf(RuntimeException.class);
                db.jdbc().update("update security_reports set payload=cast(? as jsonb)",original);
            }
            db.jdbc().execute("alter table scans drop constraint ck_scans_report_version");db.jdbc().update("update scans set report_version='report-unknown' where id=?",ids.scan());
            assertThatThrownBy(()->get.execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(life::recoverInterrupted).isInstanceOf(RuntimeException.class);
            db.jdbc().update("update scans set report_version='report-v1' where id=?",ids.scan());db.jdbc().execute("delete from security_reports");
            assertThatThrownBy(()->get.execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(life::recoverInterrupted).isInstanceOf(RuntimeException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isZero();
            assertThat(db.jdbc().queryForObject("select status from scans where id=?",String.class,ids.scan())).isEqualTo("COMPLETED");
        }
    }
    @Test void recoveryPreservesConfirmedAndInterruptsUnconfirmedWithoutGenerating() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=open(db);var confirmed=reporting(db,Verdict.VULNERABLE);c.getBean(ScanLifecyclePort.class).complete(confirmed.scan());
            var original=c.getBean(GetSecurityReportUseCase.class).execute(confirmed.project(),confirmed.scan());
            var interrupted=reporting(db,Verdict.VULNERABLE);
            assertThatThrownBy(()->tx(c).execute(s->{var score=c.getBean(CalculateScanScoreUseCase.class).execute(c.getBean(ScanScoreRepositoryPort.class).lock(interrupted.scan()));c.getBean(ScanScoreRepositoryPort.class).insert(score);c.getBean(SecurityReportRepositoryPort.class).insert(c.getBean(GenerateReportUseCase.class).execute(interrupted.scan()));throw ScanOperationException.internal();})).isInstanceOf(ScanOperationException.class);
            var gen=mock(GenerateReportUseCase.class);var life=lifecycle(c,c.getBean(SecurityReportRepositoryPort.class),gen,c.getBean(ScanRepositoryPort.class),c.getBean(PlatformTransactionManager.class));
            assertThat(life.recoverInterrupted()).isEqualTo(1);assertThat(life.recoverInterrupted()).isZero();verifyNoInteractions(gen);
            assertThat(c.getBean(GetSecurityReportUseCase.class).execute(confirmed.project(),confirmed.scan())).isEqualTo(original);
            assertThat(db.jdbc().queryForObject("select error_reason from scans where id=?",String.class,interrupted.scan())).isEqualTo("SCAN_INTERRUPTED");
            assertThat(db.jdbc().queryForObject("select count(*) from findings where scan_id=?",Integer.class,interrupted.scan())).isEqualTo(4);
            assertThat(c.getBean(GetSecurityReportUseCase.class).execute(interrupted.project(),interrupted.scan()).availability()).isEqualTo(ReportAvailability.UNAVAILABLE);
        }
    }
}
