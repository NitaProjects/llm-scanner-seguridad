package com.llmscannerseguridad.integration;

import java.nio.file.*;
import java.util.*;
import com.llmscannerseguridad.application.scan.ScanLifecyclePort;
import com.llmscannerseguridad.domain.evaluation.Verdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;
import static com.llmscannerseguridad.integration.ScoringDatabaseFixture.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
class ScoringMigrationIntegrationTest {
    @Test void v5WithLegitimateFindingsAndHistoricalScoresUpgradesWithoutBackfillOrChangingSources() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("5");var ids=seed(db,false,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE,Verdict.NOT_VULNERABLE);
            var tables=List.of("projects","target_configurations","test_cases","test_executions","evaluation_results","findings");
            var before=new HashMap<String,List<Map<String,Object>>>();
            for(String table:tables) before.put(table,db.jdbc().queryForList("select * from "+table+" order by id"));
            var scans=db.jdbc().queryForList("select * from scans");
            db.migrate("6");
            for(String table:tables) assertThat(db.jdbc().queryForList("select * from "+table+" order by id")).isEqualTo(before.get(table));
            assertThat(db.jdbc().queryForList("select id,project_id,status,started_at,finished_at,global_score,created_at,error_reason,evaluation_profile_version from scans")).isEqualTo(scans);
            assertThat(db.jdbc().queryForObject("select scoring_version from scans where id=?",String.class,ids.scan())).isNull();
            assertThat(db.jdbc().queryForObject("select count(*) from scan_scores",Integer.class)).isZero();
            assertThat(db.jdbc().queryForObject("select count(*) from information_schema.columns where table_schema=? and table_name='scan_scores' and column_name='input_hash'",Integer.class,db.schema)).isZero();
            assertThat(db.jdbc().queryForObject("select count(*) from information_schema.tables where table_schema=? and table_name='scan_score_categories'",Integer.class,db.schema)).isZero();
        }
    }
    @Test void checksRejectMalformedJsonNullsRangesMismatchedBandsCountersAndIdentity() throws Exception {
        try(var db=new ScanTestDatabase()) {
            var context=db.openSecurity(Map.of("logging.level.root","ERROR"));var ids=seed(db,true,Verdict.VULNERABLE,Verdict.VULNERABLE,Verdict.NOT_VULNERABLE,Verdict.INCONCLUSIVE);
            context.getBean(ScanLifecyclePort.class).complete(ids.scan());
            var before=db.jdbc().queryForList("select * from scan_scores");
            for(String mutation:List.of("global_score=-1","global_score=101","global_score=0","global_score=75","risk_level='LOW'","risk_level='UNKNOWN'",
                    "finding_count=-1","inconclusive_count=5","evaluation_count=3","scoring_version=''","scoring_version='scoring-v2'","calculated_at=null",
                    "scan_id='00000000-0000-0000-0000-000000000000'","severity_counts=null","severity_counts='{}'","severity_counts='[]'",
                    "severity_counts=severity_counts || '{\"OTHER\":0}'::jsonb","severity_counts=severity_counts - 'LOW'",
                    "severity_counts=jsonb_set(severity_counts,'{LOW}','null')","severity_counts=jsonb_set(severity_counts,'{LOW}','1.0')",
                    "severity_counts=jsonb_set(severity_counts,'{LOW}','\"1\"')","severity_counts=jsonb_set(severity_counts,'{LOW}','2')",
                    "categories=null","categories='[]'","categories='{}'","categories=categories - 0",
                    "categories=jsonb_set(categories,'{1}',categories->0)","categories=jsonb_set(categories,'{0,category}','\"LLM99\"')",
                    "categories=jsonb_set(categories,'{0,score}','75.1')","categories=jsonb_set(categories,'{0,score}','null')",
                    "categories=jsonb_set(categories,'{0,score}','\"75\"')","categories=jsonb_set(categories,'{0,extra}','0')",
                    "categories=jsonb_set(categories,'{0,riskLevel}','\"LOW\"')","categories=jsonb_set(categories,'{0,evaluationCount}','2')",
                    "categories=jsonb_set(categories,'{0,severityCounts,HIGH}','0')")) {
                assertThatThrownBy(()->db.jdbc().execute("update scan_scores set "+mutation)).as(mutation).isInstanceOf(RuntimeException.class);
            }
            assertThat(db.jdbc().queryForList("select * from scan_scores")).isEqualTo(before);
            assertThatThrownBy(()->db.jdbc().execute("update scans set evaluation_profile_version=null where scoring_version is not null")).isInstanceOf(RuntimeException.class);
            assertThatThrownBy(()->db.jdbc().execute("update scans set scoring_version='' where scoring_version is not null")).isInstanceOf(RuntimeException.class);
            var valid=db.jdbc().queryForObject("""
                    select scoring_v1_summary_valid(0,'NO_FINDINGS',4,0,4,0,'{"LOW":0,"MEDIUM":0,"HIGH":0,"CRITICAL":0}')
                        and scoring_v1_summary_valid(100,'CRITICAL',4,4,0,0,'{"LOW":0,"MEDIUM":0,"HIGH":0,"CRITICAL":4}')
                    """,Boolean.class);
            assertThat(valid).isTrue();
        }
    }
    @Test void packagedV1ToV5MatchSourceBytes() throws Exception {
        // Verifica recursos empaquetados frente a sus fuentes; Git se revisa además al cierre.
        try(var paths=Files.list(Path.of("src/main/resources/db/migration"))) {
            for(var file:paths.filter(p->p.getFileName().toString().matches("V[1-5]__.*\\.sql")).toList()) {
                assertThat(Files.readAllBytes(Path.of("target/classes/db/migration").resolve(file.getFileName()))).isEqualTo(Files.readAllBytes(file));
            }
        }
    }
}
