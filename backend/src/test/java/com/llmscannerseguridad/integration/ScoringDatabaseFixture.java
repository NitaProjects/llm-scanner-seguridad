package com.llmscannerseguridad.integration;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.llmscannerseguridad.domain.evaluation.Verdict;

final class ScoringDatabaseFixture {
    record Ids(UUID project, UUID scan) { }
    static final Instant TIME = Instant.parse("2026-10-10T12:00:00.123456Z");
    static Ids seed(ScanTestDatabase db, boolean marked, Verdict... verdicts) {
        var jdbc=db.jdbc();UUID project=UUID.randomUUID(),target=UUID.randomUUID(),scan=UUID.randomUUID();
        jdbc.update("insert into projects(id,name,created_at,updated_at) values(?,'scoring fixture',?,?)",project,Timestamp.from(TIME),Timestamp.from(TIME));
        jdbc.update("""
                insert into target_configurations(id,project_id,url,request_template,response_path,headers,auth_type,timeout_ms,created_at,updated_at)
                values(?,?,'https://synthetic.invalid','{"m":"{{PROMPT}}"}','$.text','{}','NONE',1000,?,?)
                """,target,project,Timestamp.from(TIME),Timestamp.from(TIME));
        jdbc.update("""
                insert into scans(id,project_id,status,created_at,started_at,evaluation_profile_version,global_score)
                values(?,?,'RUNNING',?,?,'evaluation-engine-v1',42.75)
                """,scan,project,Timestamp.from(TIME),Timestamp.from(TIME));
        if(marked) jdbc.update("update scans set scoring_version='scoring-v1' where id=?",scan);
        var tests=jdbc.queryForList("select id,category from test_cases order by code");
        for(int i=0;i<4;i++) {
            var test=tests.get(i);UUID execution=UUID.randomUUID(),evaluation=UUID.randomUUID();
            var verdict=verdicts.length==1?verdicts[0]:verdicts[i];
            String severity=verdict==Verdict.VULNERABLE?(i==0?"HIGH":"LOW"):null;
            var confidence=new java.math.BigDecimal(verdict==Verdict.INCONCLUSIVE?"0.00":"0.90");
            jdbc.update("""
                    insert into test_executions(id,scan_id,test_case_id,target_configuration_id,target_updated_at,status,started_at,finished_at,http_status,latency_ms,request_payload,response_payload)
                    values(?,?,?,?,?,'COMPLETED',?,?,200,1,'{}','"SAFE"')
                    """,execution,scan,test.get("id"),target,Timestamp.from(TIME),Timestamp.from(TIME),Timestamp.from(TIME.plusSeconds(1)));
            jdbc.update("""
                    insert into evaluation_results(id,test_execution_id,scan_id,test_case_id,category,status,created_at,started_at,finished_at,
                        final_verdict,deterministic_verdict,severity,confidence,confidence_components,decision_code,degraded,llm_status,
                        reasoning_summary,recommendation,outcome_reason)
                    values(?,?,?,?,?,'COMPLETED',?,?,?, ?,?,?,?,'{}','HYB-01',true,'SKIPPED','Resultado sintético','Revisar prueba',?)
                    """,evaluation,execution,scan,test.get("id"),test.get("category"),Timestamp.from(TIME),Timestamp.from(TIME.plusSeconds(1)),
                    Timestamp.from(TIME.plusSeconds(2)),verdict.name(),verdict.name(),severity,confidence,
                    verdict==Verdict.INCONCLUSIVE?"CONTEXT_UNAVAILABLE":null);
            if(verdict==Verdict.VULNERABLE) jdbc.update("""
                    insert into findings(id,scan_id,test_case_id,test_execution_id,evaluation_result_id,risk_level,owasp_category,
                        confidence,created_at,title,final_verdict,evidence,explanation,recommendation)
                    values(?,?,?,?,?,?,?,?,?,'Hallazgo sintético','VULNERABLE','Evidencia sintética','Resultado sintético','Revisar prueba')
                    """,UUID.randomUUID(),scan,test.get("id"),execution,evaluation,severity,test.get("category"),confidence,Timestamp.from(TIME.plusSeconds(3)));
        }
        return new Ids(project,scan);
    }
    private ScoringDatabaseFixture() { }
}
