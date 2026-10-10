package com.llmscannerseguridad.integration;

import java.nio.file.*;
import java.util.*;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static com.llmscannerseguridad.integration.ScoringDatabaseFixture.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
class ReportingMigrationIntegrationTest {
    @Test void v6WithLegitimateScoreFindingsAndActiveHistoryUpgradesWithoutBackfill() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("6");var c=db.openSecurity(Map.of("logging.level.root","ERROR","spring.flyway.target","6"),ctx -> ctx.registerBean("v6Lifecycle",ScanLifecyclePort.class,
                    () -> new com.llmscannerseguridad.config.TransactionalScanLifecycle(
                        new com.llmscannerseguridad.application.scan.ScanLifecycleService(ctx.getBean(com.llmscannerseguridad.application.project.ProjectRepositoryPort.class),ctx.getBean(com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort.class),ctx.getBean(com.llmscannerseguridad.application.scan.ScanRepositoryPort.class),java.time.Clock.systemUTC(),UUID::randomUUID),
                        ctx.getBean(org.springframework.transaction.PlatformTransactionManager.class),ctx.getBean(com.llmscannerseguridad.config.ScanExecutionGate.class)),def -> def.setPrimary(true)));
            // Composición V6 explícita, sin acceder a tablas de reporting antes de migrar.
            var scans=c.getBean(com.llmscannerseguridad.application.scan.ScanRepositoryPort.class);
            var delegate=new com.llmscannerseguridad.application.scan.ScanLifecycleService(c.getBean(com.llmscannerseguridad.application.project.ProjectRepositoryPort.class),c.getBean(com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort.class),scans,java.time.Clock.systemUTC(),UUID::randomUUID);
            var lifecycle=new com.llmscannerseguridad.config.TransactionalScanLifecycle(delegate,c.getBean(org.springframework.transaction.PlatformTransactionManager.class),c.getBean(com.llmscannerseguridad.config.ScanExecutionGate.class),c.getBean(com.llmscannerseguridad.application.execution.TestExecutionRepositoryPort.class),c.getBean(com.llmscannerseguridad.application.evaluation.EvaluationRepositoryPort.class),c.getBean(com.llmscannerseguridad.application.scoring.ScanScoreRepositoryPort.class),c.getBean(com.llmscannerseguridad.application.scoring.CalculateScanScoreUseCase.class),scans);
            var complete=seed(db,true,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE,Verdict.NOT_VULNERABLE);lifecycle.complete(complete.scan());
            var active=seed(db,true,Verdict.INCONCLUSIVE);
            var tables=List.of("test_cases","test_executions","evaluation_results","findings","scan_scores");var before=new HashMap<String,List<Map<String,Object>>>();
            for(String table:tables)before.put(table,db.jdbc().queryForList("select * from "+table+" order by 1"));
            var scansBefore=db.jdbc().queryForList("select * from scans order by id");db.closeApplication(c);db.migrate("7");
            for(String table:tables)assertThat(db.jdbc().queryForList("select * from "+table+" order by 1")).isEqualTo(before.get(table));
            var after=db.jdbc().queryForList("select * from scans order by id");for(var row:after){assertThat(row.remove("report_version")).isNull();}assertThat(after).isEqualTo(scansBefore);
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isZero();
            c=db.openSecurity(Map.of("logging.level.root","ERROR"));var get=c.getBean(GetSecurityReportUseCase.class);
            assertThat(get.execute(complete.project(),complete.scan()).availability()).isEqualTo(ReportAvailability.NOT_APPLICABLE);
            assertThat(get.execute(active.project(),active.scan()).availability()).isEqualTo(ReportAvailability.NOT_APPLICABLE);
        }
    }
    @Test void v1ToV7AndClosedSqlSchemaNullsTypesRelationsOrderingMetadataLimitsAndFks() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var c=db.openSecurity(Map.of("logging.level.root","ERROR"));var ids=seed(db,true,Verdict.VULNERABLE);db.jdbc().update("update scans set report_version='report-v1' where id=?",ids.scan());c.getBean(ScanLifecyclePort.class).complete(ids.scan());
            var before=db.jdbc().queryForList("select * from security_reports");
            var json=db.jdbc().queryForObject("select payload::text from security_reports",String.class);
            assertThat(db.jdbc().queryForObject("select report_v1_payload_valid(cast(? as jsonb))",Boolean.class,json)).isTrue();
            for(String mutation:List.of("payload=null","report_id='00000000-0000-0000-0000-000000000000'","scan_id='00000000-0000-0000-0000-000000000000'","report_version='report-v2'","scoring_version='scoring-v2'","locale='en'","generated_at=generated_at+interval '1 second'",
                    "payload='{}'","payload='[]'","payload=payload-'locale'","payload=payload || '{\"extra\":true}'::jsonb",
                    "payload=jsonb_set(payload,'{locale}','null')","payload=jsonb_set(payload,'{reportId}','\"BAD\"')","payload=jsonb_set(payload,'{generatedAt}','\"2026-10-10T12:00:00Z\"')",
                    "payload=jsonb_set(payload,'{summary,evaluationCount}','4.5')","payload=jsonb_set(payload,'{summary,categories,0,evaluationCount}','1.0')",
                    "payload=jsonb_set(payload,'{summary,categories,0,extra}','0')","payload=jsonb_set(payload,'{summary,severityCounts,HIGH}','0')",
                    "payload=jsonb_set(payload,'{evaluations,0,versions,extra}','true')","payload=jsonb_set(payload,'{evaluations,1}',payload->'evaluations'->0)",
                    "payload=jsonb_set(payload,'{findings,0,confidence}','0.42')","payload=jsonb_set(payload,'{findings,0,title}','\" \"')",
                    "payload=jsonb_set(payload,'{findings,0,title}',to_jsonb(repeat('x',151)))","payload=jsonb_set(payload,'{findings,0,description}',to_jsonb(repeat('x',1001)))",
                    "payload=jsonb_set(payload,'{interpretation,scoreMeaning}','\"Safe\"')","payload=jsonb_set(payload,'{limitations,0,text}','\"changed\"')",
                    "payload=jsonb_set(payload,'{findings}',(payload->'findings') || (payload->'findings'->0))","payload=jsonb_set(payload,'{findings,0,description}',to_jsonb(repeat('x',262145)))")) {
                assertThatThrownBy(()->db.jdbc().execute("update security_reports set "+mutation)).as(mutation).isInstanceOf(RuntimeException.class);
            }
            assertThat(db.jdbc().queryForList("select * from security_reports")).isEqualTo(before);
            assertThatThrownBy(()->db.jdbc().execute("update scans set scoring_version=null where report_version is not null")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(()->db.jdbc().execute("update scans set evaluation_profile_version=null where report_version is not null")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(()->db.jdbc().execute("delete from scan_scores")).isInstanceOf(RuntimeException.class);
            assertThat(db.jdbc().queryForObject("select max(version::integer) from flyway_schema_history where version is not null",Integer.class)).isEqualTo(7);
        }
    }
    @Test void migrationsV1ToV6ArePackagedWithoutChanges() throws Exception {
        try(var files=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(var file:files.filter(p->p.getFileName().toString().matches("V[1-6]__.*\\.sql")).toList()) assertThat(Files.readAllBytes(Path.of("target/classes/db/migration").resolve(file.getFileName()))).isEqualTo(Files.readAllBytes(file));
        }
    }
}
