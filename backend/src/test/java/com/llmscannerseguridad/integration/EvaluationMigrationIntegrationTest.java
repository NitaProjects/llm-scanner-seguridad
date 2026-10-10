package com.llmscannerseguridad.integration;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="SCAN_TEST_DB_URL",matches="jdbc:postgresql://[^/]+/llm_scanner_seguridad_test(?:\\?.*)?")
class EvaluationMigrationIntegrationTest {
    @Test void v5KeepsV4TechnicalHistoryDatesEvidenceScoresAndCatalogueWithoutBackfill() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("4");UUID scan=baseline(db);var jdbc=db.jdbc();
            var executions=jdbc.queryForList("select * from test_executions");var tests=jdbc.queryForList("select * from test_cases");
            var scans=jdbc.queryForList("select id,project_id,status,started_at,finished_at,global_score,created_at,error_reason from scans");
            db.migrate("5");
            assertThat(jdbc.queryForList("select * from test_executions")).isEqualTo(executions);
            assertThat(jdbc.queryForList("select * from test_cases")).isEqualTo(tests);
            assertThat(jdbc.queryForList("select id,project_id,status,started_at,finished_at,global_score,created_at,error_reason from scans")).isEqualTo(scans);
            assertThat(jdbc.queryForObject("select evaluation_profile_version from scans where id=?",String.class,scan)).isNull();
            assertThat(jdbc.queryForObject("select count(*) from evaluation_results",Integer.class)).isZero();
        }
    }
    @Test void incompatibleLegacyFindingsOrSemanticFieldsRollbackWholeV5WithoutDeletingHistory() throws Exception {
        for(String field:List.of("findings","evaluator_result","confidence")) {
            try(var db=new ScanTestDatabase()) {
                db.migrate("4");UUID scan=baseline(db);var jdbc=db.jdbc();
                if(field.equals("findings")) jdbc.update("insert into findings(scan_id,risk_level,owasp_category) values(?,'LOW','LLM02')",scan);
                else jdbc.execute("update test_executions set "+field+"="+(field.equals("confidence")?"80":"'legacy'"));
                var executions=jdbc.queryForList("select * from test_executions");var findings=jdbc.queryForList("select * from findings");
                assertThatThrownBy(()->db.migrate("5")).isInstanceOf(RuntimeException.class);
                assertThat(jdbc.queryForList("select * from test_executions")).isEqualTo(executions);
                assertThat(jdbc.queryForList("select * from findings")).isEqualTo(findings);
                assertThat(jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=? and table_name='scans' and column_name='evaluation_profile_version'",Integer.class,db.schema)).isZero();
                assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema=? and table_name='evaluation_results'",Integer.class,db.schema)).isZero();
            }
        }
    }
    @Test void checksRejectNullDecisionOutOfRangeLifecycleAndWrongRelations() throws Exception {
        try(var db=new ScanTestDatabase()) {
            db.migrate("4");UUID scan=baseline(db);db.migrate("5");var jdbc=db.jdbc();
            UUID execution=jdbc.queryForObject("select id from test_executions",UUID.class);UUID test=jdbc.queryForObject("select test_case_id from test_executions",UUID.class);
            UUID id=UUID.randomUUID();
            jdbc.update("insert into evaluation_results(id,test_execution_id,scan_id,test_case_id,category,status,created_at,llm_status) values(?,?,?,?,'LLM02','PENDING',CURRENT_TIMESTAMP,'NOT_REQUESTED')",id,execution,scan,test);
            for(String mutation:List.of("severity='HIGH'","confidence=1.01","llm_attempt_count=2","status='COMPLETED'","llm_status='SUCCEEDED'","category='LLM99'",
                    "test_case_id='b40e0001-0000-4000-8000-000000000001'","final_verdict='VULNERABLE'")) {
                assertThatThrownBy(()->jdbc.execute("update evaluation_results set "+mutation)).isInstanceOf(RuntimeException.class);
            }
            assertThat(jdbc.queryForObject("select status from evaluation_results",String.class)).isEqualTo("PENDING");
        }
    }
    private UUID baseline(ScanTestDatabase db) {
        var jdbc=db.jdbc();UUID project=UUID.randomUUID(),target=UUID.randomUUID(),scan=UUID.randomUUID();
        jdbc.update("insert into projects(id,name,updated_at) values(?,'fixture',CURRENT_TIMESTAMP)",project);
        jdbc.update("""
                insert into target_configurations(id,project_id,url,request_template,response_path,headers,auth_type,timeout_ms,created_at,updated_at)
                values(?,?,'https://synthetic.invalid','{"m":"{{PROMPT}}"}','$.text','{}','NONE',1000,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """,target,project);
        jdbc.update("insert into scans(id,project_id,status,created_at,started_at,finished_at,global_score) values(?,?,'COMPLETED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,42)",scan,project);
        jdbc.update("""
                insert into test_executions(id,scan_id,test_case_id,target_configuration_id,target_updated_at,status,started_at,finished_at,http_status,latency_ms,request_payload,response_payload)
                values(?,?,'b40e0002-0000-4000-8000-000000000002',?,CURRENT_TIMESTAMP,'COMPLETED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,200,1,'{}','"SAFE"')
                """,UUID.randomUUID(),scan,target);
        return scan;
    }
}
