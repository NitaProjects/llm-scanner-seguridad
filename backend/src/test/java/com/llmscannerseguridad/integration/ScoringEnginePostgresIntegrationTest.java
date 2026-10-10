package com.llmscannerseguridad.integration;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import com.llmscannerseguridad.adapters.in.rest.scan.*;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.*;
import com.llmscannerseguridad.application.scoring.*;
import com.llmscannerseguridad.config.*;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.domain.scan.*;
import com.llmscannerseguridad.domain.scoring.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static com.llmscannerseguridad.integration.ScoringDatabaseFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
@Timeout(90)
class ScoringEnginePostgresIntegrationTest {
    private ConfigurableApplicationContext open(ScanTestDatabase db) { return db.openSecurity(Map.of("logging.level.root","ERROR")); }
    private TransactionTemplate transaction(ConfigurableApplicationContext context) { return new TransactionTemplate(context.getBean(PlatformTransactionManager.class)); }
    @Test void zeroMixedAndInconclusiveRoundTripScopedApiAndNewInstanceReconstruction() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var lifecycle=context.getBean(ScanLifecyclePort.class);var get=context.getBean(GetScanScoreUseCase.class);
            for(var verdicts:List.of(new Verdict[]{Verdict.NOT_VULNERABLE},new Verdict[]{Verdict.INCONCLUSIVE},
                    new Verdict[]{Verdict.VULNERABLE,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE})) {
                var ids=seed(db,true,verdicts);
                assertThat(get.execute(ids.project(),ids.scan()).availability()).isEqualTo(ScoreAvailability.PENDING);
                var original=db.jdbc().queryForList("select * from findings where scan_id=? order by id",ids.scan());
                var evaluations=db.jdbc().queryForList("select * from evaluation_results where scan_id=? order by id",ids.scan());
                var candidate=transaction(context).execute(s->context.getBean(CalculateScanScoreUseCase.class).execute(context.getBean(ScanScoreRepositoryPort.class).lock(ids.scan())));
                assertThat(lifecycle.complete(ids.scan())).isTrue();
                var listing=get.execute(ids.project(),ids.scan());assertThat(listing.availability()).isEqualTo(ScoreAvailability.AVAILABLE);
                assertThat(listing.score().sameResult(candidate)).isTrue();
                assertThat(listing.score().globalScore()).isEqualTo(verdicts.length==1?0:76);
                assertThat(listing.score().inconclusiveCount()).isEqualTo(verdicts[0]==Verdict.INCONCLUSIVE?4:verdicts.length==1?0:1);
                assertThat(lifecycle.complete(ids.scan())).isTrue();assertThat(get.execute(ids.project(),ids.scan())).isEqualTo(listing);
                assertThat(db.jdbc().queryForList("select * from findings where scan_id=? order by id",ids.scan())).isEqualTo(original);
                assertThat(db.jdbc().queryForList("select * from evaluation_results where scan_id=? order by id",ids.scan())).isEqualTo(evaluations);
                assertThat(db.jdbc().queryForObject("select global_score from scans where id=?",java.math.BigDecimal.class,ids.scan())).isEqualByComparingTo("42.75");
                var mvc=MockMvcBuilders.standaloneSetup(context.getBean(ScoreController.class),context.getBean(ScanController.class),
                        context.getBean(EvaluationController.class),context.getBean(TestExecutionController.class))
                        .setControllerAdvice(new EvaluationExceptionHandler(),new ScanExceptionHandler()).build();
                String base="/api/projects/"+ids.project()+"/scans/"+ids.scan();
                mvc.perform(get(base+"/score")).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("AVAILABLE"))
                        .andExpect(jsonPath("$.score.globalScore").value(listing.score().globalScore())).andExpect(header().string("Cache-Control","no-store"));
                mvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.aMapWithSize(7)));
                for(String suffix:List.of("executions","evaluations","findings")) mvc.perform(get(base+"/"+suffix)).andExpect(status().isOk());
                mvc.perform(get(base.replace(ids.project().toString(),UUID.randomUUID().toString())+"/score")).andExpect(status().isNotFound());
            }
            var confirmed=db.jdbc().queryForList("select * from scan_scores order by scan_id");
            db.closeApplication(context);var reopened=open(db);
            assertThat(db.jdbc().queryForList("select * from scan_scores order by scan_id")).isEqualTo(confirmed);
            assertThat(reopened.getBean(ScanScoreRepositoryPort.class)).isNotNull();
        }
    }
    @Test void concurrentCloseAndDuplicateCandidatesAreImmutableAndDatabaseUnique() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var ids=seed(db,true,Verdict.VULNERABLE);var lifecycle=context.getBean(ScanLifecyclePort.class);
            try(var pool=Executors.newFixedThreadPool(2)) {
                var start=new CountDownLatch(1);
                var a=pool.submit(()->{start.await();return lifecycle.complete(ids.scan());});
                var b=pool.submit(()->{start.await();return lifecycle.complete(ids.scan());});start.countDown();
                assertThat(a.get()).isTrue();assertThat(b.get()).isTrue();
            }
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isEqualTo(1);
            var repo=context.getBean(ScanScoreRepositoryPort.class);var score=context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).score();
            var later=new ScanScore(score.scanId(),score.scoringVersion(),score.calculatedAt().plusSeconds(10),score.globalScore(),score.riskLevel(),
                    score.evaluationCount(),score.findingCount(),score.notVulnerableCount(),score.inconclusiveCount(),score.severityCounts(),score.categories());
            ScanScore reused=transaction(context).execute(s->repo.insert(later));
            assertThat(reused).isEqualTo(score);
            var categories=new ArrayList<>(score.categories());var first=categories.getFirst();
            categories.set(0,new CategoryScore(first.category(),74,RiskLevel.HIGH,1,1,0,0,first.severityCounts()));
            var divergent=new ScanScore(score.scanId(),score.scoringVersion(),score.calculatedAt(),78,RiskLevel.HIGH,4,4,0,0,score.severityCounts(),categories);
            assertThatThrownBy(()->transaction(context).execute(s->repo.insert(divergent))).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->db.jdbc().execute("insert into scan_scores select * from scan_scores")).isInstanceOf(RuntimeException.class);
            assertThat(context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).score()).isEqualTo(score);
        }
    }
    @Test void failureAfterInsertAndBeforeTransitionRollsBackThenCompensatesWithoutChangingFindings() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var ids=seed(db,true,Verdict.VULNERABLE);
            var repository=spy(context.getBean(ScanScoreRepositoryPort.class));
            doAnswer(call->{call.callRealMethod();assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
                throw new IllegalStateException("SYNTHETIC_INSERT_FAILURE");}).when(repository).insert(any());
            var failing=lifecycle(context,repository,context.getBean(CalculateScanScoreUseCase.class),context.getBean(ScanRepositoryPort.class));
            assertThatThrownBy(()->failing.complete(ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
            assertThat(db.jdbc().queryForObject("select status from scans where id=?",String.class,ids.scan())).isEqualTo("RUNNING");
            assertThat(failing.fail(ids.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            assertThat(context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ScoreAvailability.UNAVAILABLE);
            assertThat(db.jdbc().queryForObject("select count(*) from findings",Integer.class)).isEqualTo(4);

            var second=seed(db,true,Verdict.NOT_VULNERABLE);
            var scans=mock(ScanRepositoryPort.class,org.mockito.AdditionalAnswers.delegatesTo(context.getBean(ScanRepositoryPort.class)));
            doReturn(false).when(scans).transition(eq(ScanStatus.RUNNING),any());
            assertThatThrownBy(()->lifecycle(context,context.getBean(ScanScoreRepositoryPort.class),context.getBean(CalculateScanScoreUseCase.class),scans).complete(second.scan()))
                    .isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
            var duringCalculation=seed(db,true,Verdict.VULNERABLE);
            var inputFailure=new CalculateScanScoreUseCase(id->{throw new IllegalStateException("SYNTHETIC_CALCULATION_INTERRUPTION");},Clock.systemUTC());
            assertThatThrownBy(()->lifecycle(context,context.getBean(ScanScoreRepositoryPort.class),inputFailure,context.getBean(ScanRepositoryPort.class)).complete(duringCalculation.scan()))
                    .isInstanceOf(ScanOperationException.class);
            db.closeApplication(context);open(db);
            assertThat(db.jdbc().queryForObject("select error_reason from scans where id=?",String.class,second.scan())).isEqualTo("SCAN_INTERRUPTED");
            assertThat(db.jdbc().queryForObject("select error_reason from scans where id=?",String.class,duringCalculation.scan())).isEqualTo("SCAN_INTERRUPTED");
            assertThat(db.jdbc().queryForObject("select count(*) from findings where scan_id=?",Integer.class,duringCalculation.scan())).isEqualTo(4);
        }
    }
    @Test void missingOrInconsistentInputFailsTechnicallyAndRecoveryPreservesConfirmedAndInterruptedSources() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var active=seed(db,true,Verdict.VULNERABLE);var completed=seed(db,true,Verdict.NOT_VULNERABLE);
            var lifecycle=context.getBean(ScanLifecyclePort.class);lifecycle.complete(completed.scan());
            var before=db.jdbc().queryForList("select * from scan_scores");
            var missing=seed(db,true,Verdict.NOT_VULNERABLE);
            db.jdbc().update("delete from evaluation_results where id=(select id from evaluation_results where scan_id=? order by id limit 1)",missing.scan());
            assertThatThrownBy(()->lifecycle.complete(missing.scan())).isInstanceOf(ScanOperationException.class);
            assertThat(lifecycle.fail(missing.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).isTrue();
            // Fuentes terminales válidas con fechas incoherentes no se arreglan silenciosamente.
            var bad=seed(db,true,Verdict.VULNERABLE);
            db.jdbc().update("update findings set created_at=? where scan_id=?",java.sql.Timestamp.from(TIME),bad.scan());
            assertThatThrownBy(()->lifecycle.complete(bad.scan())).isInstanceOf(ScanOperationException.class);
            lifecycle.fail(bad.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED);
            var findings=db.jdbc().queryForList("select * from findings order by id");
            db.closeApplication(context);var recovered=open(db);
            assertThat(db.jdbc().queryForObject("select error_reason from scans where id=?",String.class,active.scan())).isEqualTo("SCAN_INTERRUPTED");
            assertThat(db.jdbc().queryForList("select * from scan_scores")).isEqualTo(before);
            assertThat(db.jdbc().queryForList("select * from findings order by id")).isEqualTo(findings);
            assertThat(recovered.getBean(GetScanScoreUseCase.class).execute(active.project(),active.scan()).availability()).isEqualTo(ScoreAvailability.UNAVAILABLE);
        }
    }
    @Test void durableCorruptionIsNotRepairedByGetCloseOrRecovery() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var ids=seed(db,true,Verdict.NOT_VULNERABLE);
            db.jdbc().update("update scans set status='COMPLETED',finished_at=? where id=?",java.sql.Timestamp.from(TIME.plusSeconds(3)),ids.scan());
            assertThatThrownBy(()->context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->context.getBean(ScanLifecyclePort.class).complete(ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->context.getBean(ScanLifecyclePort.class).recoverInterrupted()).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
            // Una fila durable asociada a un Scan activo también es corrupción.
            db.jdbc().update("update scans set status='RUNNING',finished_at=null where id=?",ids.scan());
            context.getBean(ScanLifecyclePort.class).complete(ids.scan());
            db.jdbc().update("update scans set status='RUNNING',finished_at=null where id=?",ids.scan());
            var row=db.jdbc().queryForList("select * from scan_scores");
            assertThatThrownBy(()->context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->context.getBean(ScanLifecyclePort.class).recoverInterrupted()).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForList("select * from scan_scores")).isEqualTo(row);
            db.jdbc().update("update scans set status='FAILED',finished_at=?,error_reason='SCAN_PROCESSING_FAILED' where id=?",java.sql.Timestamp.from(Instant.now()),ids.scan());
            assertThatThrownBy(()->context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan())).isInstanceOf(ScanOperationException.class);
            assertThatThrownBy(()->context.getBean(ScanLifecyclePort.class).recoverInterrupted()).isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForList("select * from scan_scores")).isEqualTo(row);
        }
    }
    @Test void realWorkerCreatesMarkerAndCompletesFourInconclusivesWithoutProviderCalls() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=db.openSecurity(Map.of("logging.level.root","ERROR"),app->app.registerBean("scoringFakeTarget",TargetInvocationPort.class,
                    ()->(target,prompt,budget)->new InvocationResult(200,1L,null,"{}","SAFE",false,false),definition->definition.setPrimary(true)));
            var history=seed(db,false,Verdict.NOT_VULNERABLE);
            context.getBean(ScanLifecyclePort.class).complete(history.scan());
            assertThat(context.getBean(GetScanScoreUseCase.class).execute(history.project(),history.scan()).availability()).isEqualTo(ScoreAvailability.NOT_APPLICABLE);
            var lifecycle=context.getBean(ScanLifecyclePort.class);var scan=lifecycle.create(history.project());
            assertThat(db.jdbc().queryForObject("select scoring_version from scans where id=?",String.class,scan.id())).isEqualTo("scoring-v1");
            context.getBean(ScanWorkerEntry.class).execute(scan.id());
            var score=context.getBean(GetScanScoreUseCase.class).execute(scan.projectId(),scan.id());
            assertThat(score.availability()).isEqualTo(ScoreAvailability.AVAILABLE);assertThat(score.score().globalScore()).isZero();
            assertThat(score.score().inconclusiveCount()).isEqualTo(4);
            var beforePlan=lifecycle.create(history.project());
            lifecycle.fail(beforePlan.id(),ScanStatus.PENDING,ScanFailureReason.SCAN_PROCESSING_FAILED);
            assertThat(context.getBean(GetScanScoreUseCase.class).execute(history.project(),beforePlan.id()).availability()).isEqualTo(ScoreAvailability.UNAVAILABLE);
            assertThat(db.jdbc().queryForObject("select count(*) from evaluation_results where scan_id=?",Integer.class,beforePlan.id())).isZero();
            var historicalFailed=seed(db,false,Verdict.NOT_VULNERABLE);
            lifecycle.fail(historicalFailed.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED);
            assertThat(context.getBean(GetScanScoreUseCase.class).execute(historicalFailed.project(),historicalFailed.scan()).availability()).isEqualTo(ScoreAvailability.NOT_APPLICABLE);
        }
    }
    @Test void pollingAcrossUncommittedInsertSeesPendingThenAvailableWithoutFalseCorruption() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var ids=seed(db,true,Verdict.NOT_VULNERABLE);
            var repository=spy(context.getBean(ScanScoreRepositoryPort.class));
            var inserted=new CountDownLatch(1);var release=new CountDownLatch(1);
            doAnswer(call->{var result=call.callRealMethod();inserted.countDown();
                if(!release.await(10,TimeUnit.SECONDS)) throw new IllegalStateException("Test timeout");return result;}).when(repository).insert(any());
            var lifecycle=lifecycle(context,repository,context.getBean(CalculateScanScoreUseCase.class),context.getBean(ScanRepositoryPort.class));
            try(var pool=Executors.newSingleThreadExecutor()) {
                var close=pool.submit(()->lifecycle.complete(ids.scan()));
                try {
                    assertThat(inserted.await(10,TimeUnit.SECONDS)).isTrue();
                    for(int i=0;i<20;i++) assertThat(context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ScoreAvailability.PENDING);
                } finally { release.countDown(); }
                assertThat(close.get()).isTrue();
                assertThat(context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).availability()).isEqualTo(ScoreAvailability.AVAILABLE);
            }
        }
    }
    @Test void ambiguousCommitPreservesConfirmedPairOrCompensatesRolledBackPair() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);
            for(boolean confirmed:List.of(false,true)) {
                var ids=seed(db,true,Verdict.VULNERABLE);
                var real=context.getBean(PlatformTransactionManager.class);
                var manager=mock(PlatformTransactionManager.class,org.mockito.AdditionalAnswers.delegatesTo(real));
                var first=new java.util.concurrent.atomic.AtomicBoolean(true);
                doAnswer(call->{
                    org.springframework.transaction.TransactionStatus status=call.getArgument(0);
                    if(first.getAndSet(false)) {
                        if(confirmed) real.commit(status);else real.rollback(status);
                        throw new org.springframework.transaction.TransactionSystemException("SYNTHETIC_AMBIGUOUS_COMMIT");
                    }
                    real.commit(status);return null;
                }).when(manager).commit(any());
                var lifecycle=new TransactionalScanLifecycle(new ScanLifecycleService(context.getBean(ProjectRepositoryPort.class),
                        context.getBean(com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort.class),context.getBean(ScanRepositoryPort.class),Clock.systemUTC(),UUID::randomUUID),
                        manager,context.getBean(ScanExecutionGate.class),context.getBean(TestExecutionRepositoryPort.class),
                        context.getBean(com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort.class),context.getBean(ScanScoreRepositoryPort.class),
                        context.getBean(CalculateScanScoreUseCase.class),context.getBean(ScanRepositoryPort.class));
                assertThatThrownBy(()->lifecycle.complete(ids.scan())).isInstanceOf(ScanOperationException.class);
                assertThat(lifecycle.fail(ids.scan(),ScanStatus.RUNNING,ScanFailureReason.SCAN_PROCESSING_FAILED)).isEqualTo(!confirmed);
                var listing=context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan());
                assertThat(listing.availability()).isEqualTo(confirmed?ScoreAvailability.AVAILABLE:ScoreAvailability.UNAVAILABLE);
                assertThat(db.jdbc().queryForObject("select count(*) from findings where scan_id=?",Integer.class,ids.scan())).isEqualTo(4);
                db.closeApplication(context);context=open(db);
                assertThat(context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan())).isEqualTo(listing);
            }
        }
    }
    @Test void creationMarkerFailureRollsBackEntireT1AndFutureInputClockCannotRegressCompletion() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=open(db);var history=seed(db,false,Verdict.NOT_VULNERABLE);context.getBean(ScanLifecyclePort.class).complete(history.scan());
            var repository=spy(context.getBean(ScanScoreRepositoryPort.class));
            doAnswer(call->{call.callRealMethod();throw new IllegalStateException("SYNTHETIC_MARKER_FAILURE");}).when(repository).setRequiredVersion(any());
            assertThatThrownBy(()->lifecycle(context,repository,context.getBean(CalculateScanScoreUseCase.class),context.getBean(ScanRepositoryPort.class)).create(history.project()))
                    .isInstanceOf(ScanOperationException.class);
            assertThat(db.jdbc().queryForObject("select count(*) from scans",Integer.class)).isEqualTo(1);
            var ids=seed(db,true,Verdict.NOT_VULNERABLE);
            var future=TIME.plusSeconds(365L*24*60*60);
            var calculator=new CalculateScanScoreUseCase(context.getBean(ScoringInputPort.class),Clock.fixed(future,ZoneOffset.UTC));
            assertThat(lifecycle(context,context.getBean(ScanScoreRepositoryPort.class),calculator,context.getBean(ScanRepositoryPort.class)).complete(ids.scan())).isTrue();
            var score=context.getBean(GetScanScoreUseCase.class).execute(ids.project(),ids.scan()).score();
            assertThat(score.calculatedAt()).isEqualTo(future);
            assertThat(context.getBean(GetScanUseCase.class).execute(ids.project(),ids.scan()).finishedAt()).isAfterOrEqualTo(future);
        }
    }
    private TransactionalScanLifecycle lifecycle(ConfigurableApplicationContext context,ScanScoreRepositoryPort repository,
            CalculateScanScoreUseCase calculate,ScanRepositoryPort scans) {
        return new TransactionalScanLifecycle(new ScanLifecycleService(context.getBean(ProjectRepositoryPort.class),
                context.getBean(com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort.class),context.getBean(ScanRepositoryPort.class),
                Clock.systemUTC(),UUID::randomUUID),context.getBean(PlatformTransactionManager.class),context.getBean(ScanExecutionGate.class),
                context.getBean(TestExecutionRepositoryPort.class),context.getBean(com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort.class),
                repository,calculate,scans);
    }
}
