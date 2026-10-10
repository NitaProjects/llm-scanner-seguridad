package com.llmscannerseguridad.domain.reporting;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.RecordComponent;
import java.util.*;
import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.adapters.out.reporting.LocalReportPresentationAdapter;
import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.domain.evaluation.*;
import org.junit.jupiter.api.Test;
import static com.llmscannerseguridad.application.reporting.ReportingFixtures.*;
import static org.assertj.core.api.Assertions.*;

class SecurityReportTest {
    @SuppressWarnings("unchecked") private Map<String,Object> object(Object v) { return (Map<String,Object>)v; }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> entries(Map<String,Object> p,String key) { return (List<Map<String,Object>>)p.get(key); }
    private Map<String,Object> payload(SecurityReport r) { return object(ReportJson.read(ReportJson.write(r),Map.class)); }
    private void invalid(Map<String,Object> p) { assertThatThrownBy(()->ReportJson.read(ReportJson.write(p),SecurityReport.class)).isInstanceOf(RuntimeException.class); }
    @Test void exactShapeImmutableReferencesAndStrictUtcRoundTrip() {
        var r=report(inputs(Verdict.VULNERABLE));var json=ReportJson.write(r);assertThat(ReportJson.read(json,SecurityReport.class)).isEqualTo(r);
        var p=payload(r);assertThat(p).hasSize(12);assertThat(entries(p,"evaluations").getFirst()).hasSize(19);
        assertThat(object(entries(p,"evaluations").getFirst().get("versions"))).hasSize(7);
        assertThat(entries(p,"findings").getFirst()).hasSize(15);assertThat(object(p.get("summary"))).hasSize(11);
        assertThat(object(p.get("scope"))).hasSize(4);assertThat(object(p.get("interpretation"))).hasSize(4);
        assertThatThrownBy(()->r.findings().clear()).isInstanceOf(UnsupportedOperationException.class);
        for(String field:List.of("rawEvidence","prompts","llmAssessment","deterministicSignals","executiveSummaryText")) {p=payload(r);p.put(field,"SYNTHETIC_PRIVATE");invalid(p);}
        p=payload(r);p.remove("locale");invalid(p);
        for(String at:List.of("2026-10-10T12:00:03Z","2026-10-10T12:00:03.123456+00:00","2026-13-10T12:00:03.123456Z")) {p=payload(r);p.put("generatedAt",at);invalid(p);}
        p=payload(r);p.put("reportId",r.reportId().toString().toUpperCase(Locale.ROOT));invalid(p);
        assertThatThrownBy(()->ReportJson.read(json.replaceFirst("\\{","{\"locale\":\"es\","),SecurityReport.class)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->ReportJson.read(json+"{}",SecurityReport.class)).isInstanceOf(RuntimeException.class);
    }
    @Test void sourceTextCodePointBoundariesUnicodeAndNoSilentTruncation() {
        var r=report(inputs(Verdict.VULNERABLE));
        for(String field:List.of("title","description","evidenceSummary","recommendation")) {
            int max=field.equals("title")?150:1000;var p=payload(r);
            entries(p,"findings").getFirst().put(field,"😀".repeat(max));
            var accepted=ReportJson.read(ReportJson.write(p),SecurityReport.class);assertThat(accepted.findings()).hasSize(4);
            entries(p,"findings").getFirst().put(field,"😀".repeat(max+1));invalid(p);
            for(String bad:List.of(" ","\u0000","<tag>","`code`","\uD800","\uDC00")) {
                assertThatThrownBy(()->ReportRules.text(bad,max)).isInstanceOf(IllegalArgumentException.class);
            }
        }
        for(String field:List.of("criteriaId","criteriaVersion")) {
            int max=field.equals("criteriaId")?64:32;var p=payload(r);entries(p,"evaluations").getFirst().put(field,"a".repeat(max));
            assertThat(ReportJson.read(ReportJson.write(p),SecurityReport.class)).isNotNull();entries(p,"evaluations").getFirst().put(field,"a".repeat(max+1));invalid(p);
        }
        var p=payload(r);var versions=object(entries(p,"evaluations").getFirst().get("versions"));
        for(var key:versions.keySet()) versions.put(key,"v".repeat(64));assertThat(ReportJson.read(ReportJson.write(p),SecurityReport.class)).isNotNull();
        versions.put("engineVersion","v".repeat(65));invalid(p);
        p=payload(r);entries(p,"findings").add(entries(p,"findings").getFirst());invalid(p);
    }
    @Test void maximumCompleteFixtureAndIsolatedByteBoundaries() {
        var r=report(inputs(Verdict.VULNERABLE));var p=payload(r);
        for(var f:entries(p,"findings")) {
            f.put("title","😀".repeat(150));for(String field:List.of("description","evidenceSummary","recommendation")) f.put(field,"\"\\😀".repeat(333)+"😀");
        }
        var full=ReportJson.read(ReportJson.write(p),SecurityReport.class);
        int size=ReportJson.write(full).getBytes(StandardCharsets.UTF_8).length;assertThat(size).isLessThan(262144);
        ReportJson.guard(262144,262144);assertThatThrownBy(()->ReportJson.guard(262145,262144)).isInstanceOf(RuntimeException.class);
        ReportJson.guard(512,512);assertThatThrownBy(()->ReportJson.guard(513,512)).isInstanceOf(RuntimeException.class);
        String wrapped=ReportJson.response(Map.of("availability","AVAILABLE","report",full),full);
        assertThat(wrapped.getBytes(StandardCharsets.UTF_8).length-size).isLessThanOrEqualTo(512);
    }
    @Test void countersLinksOrderingAndSummaryDisagreementAreRejected() {
        var r=report(inputs(Verdict.VULNERABLE));var p=payload(r);Collections.reverse(entries(p,"findings"));invalid(p);
        p=payload(r);Collections.reverse(entries(p,"evaluations"));invalid(p);
        for(String field:List.of("evaluationResultId","testExecutionId","scanId")) {p=payload(r);entries(p,"findings").getFirst().put(field,UUID.randomUUID().toString());invalid(p);}
        p=payload(r);entries(p,"findings").getFirst().put("confidence",0.42);invalid(p);
        p=payload(r);object(p.get("summary")).put("findingCount",3);invalid(p);
        p=payload(r);object(p.get("summary")).put("evaluationCount",4.5);invalid(p);
        p=payload(r);entries(p,"evaluations").set(1,entries(p,"evaluations").getFirst());invalid(p);
    }
    @Test void comparatorHasExplicitSeverityAndUnsignedLexicalUuidTieBreak() {
        var r=report(inputs(new Severity[]{Severity.CRITICAL,Severity.HIGH,Severity.MEDIUM,Severity.LOW},Verdict.VULNERABLE));
        assertThat(r.findings().stream().map(ReportFinding::severity)).containsExactly(Severity.CRITICAL,Severity.HIGH,Severity.MEDIUM,Severity.LOW);
        var f=r.findings().getFirst();var p=payload(r);var a=new HashMap<>(entries(p,"findings").getFirst());var b=new HashMap<>(a);
        a.put("id","00000000-0000-0000-0000-000000000001");b.put("id","ffffffff-ffff-ffff-ffff-ffffffffffff");
        var left=ReportJson.read(ReportJson.write(a),ReportFinding.class);var right=ReportJson.read(ReportJson.write(b),ReportFinding.class);
        assertThat(ReportFinding.ORDER.compare(left,right)).isNegative();assertThat(ReportFinding.ORDER.compare(right,left)).isPositive();
        a.put("severity","HIGH");a.put("confidence",0.60);b.put("severity","MEDIUM");b.put("confidence",0.95);
        assertThat(ReportFinding.ORDER.compare(ReportJson.read(ReportJson.write(a),ReportFinding.class),ReportJson.read(ReportJson.write(b),ReportFinding.class))).isNegative();
        assertThat(f.toString()).doesNotContain(f.title(),f.recommendation());
    }
    @Test void controlledResourceMatchesApprovedLiteralTextsAndLabelsAndRejectsBadResources() throws Exception {
        var adapter=new LocalReportPresentationAdapter();var p=adapter.load("report-v1","es");
        assertThat(p.labels().severity().get("HIGH")).isEqualTo("Alta");assertThat(p.labels().riskLevel().get("HIGH")).isEqualTo("Alto");
        var design=Files.readString(Path.of("../openspec/changes/reporting/design.md"));
        p.limitations().forEach(l->assertThat(design).contains(l.code()+" | "+l.text()+" |"));
        for(var field:SecurityReport.Interpretation.class.getRecordComponents()) assertThat(design).contains((String)field.getAccessor().invoke(p.interpretation()));
        assertThatThrownBy(()->adapter.load("report-v2","es")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->adapter.load("report-v1","en")).isInstanceOf(RuntimeException.class);
        for(String bad:List.of("{}","null","{\"unknown\":true}")) assertThatThrownBy(()->new LocalReportPresentationAdapter(()->bad).load("report-v1","es")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->new LocalReportPresentationAdapter(()->null).load("report-v1","es")).isInstanceOf(RuntimeException.class);
    }
    @Test void reportingDomainAndApplicationNeverImportFrameworksOrScoringPolicy() throws Exception {
        for(String dir:List.of("domain/reporting","application/reporting")) try(var files=Files.list(Path.of("src/main/java/com/llmscannerseguridad/"+dir))) {
            for(var file:files.toList()) assertThat(Files.readString(file)).doesNotContain("org.springframework","jakarta.persistence","tools.jackson","ScoringPolicyV1","new ScanScore(","EvaluationResult(");
        }
        assertThat(SecurityReport.class.getRecordComponents()).hasSize(12);assertThat(ReportEvaluation.class.getRecordComponents()).hasSize(19);
        assertThat(ReportEvaluation.Versions.class.getRecordComponents()).hasSize(7);
        assertThat(Arrays.stream(ReportEvaluation.class.getRecordComponents()).map(RecordComponent::getName)).doesNotContain("reasoningSummary","deterministicSignals","llmAssessment");
    }
}
