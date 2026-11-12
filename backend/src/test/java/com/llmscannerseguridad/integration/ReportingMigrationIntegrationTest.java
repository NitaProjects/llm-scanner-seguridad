package com.llmscannerseguridad.integration;

import java.nio.file.*;
import java.util.*;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.adapters.out.persistence.scoring.PostgresScanScoreRepositoryAdapter;
import com.llmscannerseguridad.adapters.out.persistence.scoring.PostgresScoringInputAdapter;
import com.llmscannerseguridad.application.scoring.CalculateScanScoreUseCase;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static com.llmscannerseguridad.integration.ScoringDatabaseFixture.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
class ReportingMigrationIntegrationTest {
    @Test void v6WithLegitimateScoreFindingsAndActiveHistoryUpgradesWithoutBackfill() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("6");
            // Preparar historia V6 con el cálculo real JDBC, sin iniciar entidades JPA de V8.
            var complete=seed(db,true,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE,Verdict.NOT_VULNERABLE);
            var source=db.jdbc().getDataSource();
            var scores=new PostgresScanScoreRepositoryAdapter(source);
            var calculate=new CalculateScanScoreUseCase(new PostgresScoringInputAdapter(source),java.time.Clock.systemUTC());
            new TransactionTemplate(new DataSourceTransactionManager(source)).executeWithoutResult(status -> {
                var score=scores.insert(calculate.execute(scores.lock(complete.scan())));
                assertThat(new org.springframework.jdbc.core.JdbcTemplate(source).update("update scans set status='COMPLETED',finished_at=? where id=? and status='RUNNING'",
                        java.sql.Timestamp.from(score.calculatedAt()),complete.scan())).isOne();
            });
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isOne();
            var active=seed(db,true,Verdict.INCONCLUSIVE);
            var tables=List.of("test_cases","test_executions","evaluation_results","findings","scan_scores");var before=new HashMap<String,List<Map<String,Object>>>();
            for(String table:tables)before.put(table,db.jdbc().queryForList("select * from "+table+" order by 1"));
            var scansBefore=db.jdbc().queryForList("select * from scans order by id");db.migrate("7");
            for(String table:tables)assertThat(db.jdbc().queryForList("select * from "+table+" order by 1")).isEqualTo(before.get(table));
            var after=db.jdbc().queryForList("select * from scans order by id");for(var row:after){assertThat(row.remove("report_version")).isNull();}assertThat(after).isEqualTo(scansBefore);
            assertThat(db.jdbc().queryForObject("select count(*) from security_reports",Integer.class)).isZero();
            var targetsBefore=db.jdbc().queryForList("select * from target_configurations order by id");
            var scansV7=db.jdbc().queryForList("select * from scans order by id");
            db.migrate("8");
            var targetsAfter=db.jdbc().queryForList("select * from target_configurations order by id");
            for(var row:targetsAfter) {
                assertThat(row.remove("adapter_type")).isEqualTo("REST_LEGACY");
                assertThat(row.remove("model")).isNull();
                assertThat(row.remove("system_prompt")).isNull();
            }
            assertThat(targetsAfter).isEqualTo(targetsBefore);
            for(String table:tables)assertThat(db.jdbc().queryForList("select * from "+table+" order by 1")).isEqualTo(before.get(table));
            assertThat(db.jdbc().queryForList("select * from scans order by id")).isEqualTo(scansV7);
            var c=db.openSecurity(Map.of("logging.level.root","ERROR"));var get=c.getBean(GetSecurityReportUseCase.class);
            assertThat(get.execute(complete.project(),complete.scan()).availability()).isEqualTo(ReportAvailability.NOT_APPLICABLE);
            assertThat(get.execute(active.project(),active.scan()).availability()).isEqualTo(ReportAvailability.NOT_APPLICABLE);
        }
    }
    @Test void v1ToV8AndClosedSqlSchemaNullsTypesRelationsOrderingMetadataLimitsAndFks() throws Exception {
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
            assertThat(db.jdbc().queryForObject("select max(version::integer) from flyway_schema_history where version is not null",Integer.class)).isEqualTo(8);
        }
    }
    @Test void migrationsV1ToV6ArePackagedWithoutChanges() throws Exception {
        try(var files=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(var file:files.filter(p->p.getFileName().toString().matches("V[1-6]__.*\\.sql")).toList()) assertThat(Files.readAllBytes(Path.of("target/classes/db/migration").resolve(file.getFileName()))).isEqualTo(Files.readAllBytes(file));
        }
    }
}
