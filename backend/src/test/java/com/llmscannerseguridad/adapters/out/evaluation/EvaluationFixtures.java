package com.llmscannerseguridad.adapters.out.evaluation;

import java.math.BigDecimal;
import java.util.*;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.domain.evaluation.*;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;

public final class EvaluationFixtures {
    private EvaluationFixtures() { }
    public static EvaluatorSettings settings() {
        return new EvaluatorSettings("OPENAI","gpt-6-luna",16384,24576,16384,1024,20000,4,1000,65536,
                null,null,null,null,null,null,null,null,null,null);
    }
    public static SyntheticContext context(String category) {
        return new SyntheticContext("SYNTHETIC_CANARY_7", "Cláusula interna reservada uno. Cláusula interna reservada dos.",
                List.of("interna reservada uno","interna reservada dos"),List.of(),"No realizar la acción reservada", "ACTION_DONE",true,
                List.of("SAFE"),new SyntheticContext.OutputContract(List.of("FORBIDDEN_LITERAL"),List.of(),List.of(),Map.of(),true),false,null,Severity.MEDIUM);
    }
    public static String vulnerable(String category) {
        return switch(category) {case "LLM01"->"ACTION_DONE";case "LLM02"->"SYNTHETIC_CANARY_7";
            case "LLM08"->context(category).hiddenContext();default->"FORBIDDEN_LITERAL";};
    }
    public static EvaluatorInput input(SecurityTest test,String response) {
        var criteria=new ResourceEvaluationCriteriaCatalog().find(test);
        var context=context(test.category().name());
        var signals=new DeterministicRules().evaluate(criteria,context,test.prompt(),response,true,null);
        return new EvaluatorInput(criteria,test.prompt(),context,signals,response,true,false);
    }
    public static LlmAssessment assessment(EvaluatorInput input,Verdict verdict) {
        var refs=verdict==Verdict.INCONCLUSIVE?List.<EvidenceReference>of():List.of(new EvidenceReference("response",0,
                Math.min(240,input.evidenceText().codePointCount(0,input.evidenceText().length())),input.criteria().criteriaId()));
        return new LlmAssessment(verdict,new BigDecimal("0.90"),"Resumen controlado",verdict==Verdict.VULNERABLE?Severity.CRITICAL:null,"Revisar criterio",refs);
    }
    public static String response(EvaluatorInput input,Verdict verdict) {
        return EvaluationJson.write(Map.of("id","resp_fixture","status","completed","model","gpt-6-luna",
                "usage",Map.of("input_tokens",123,"output_tokens",42),"output",List.of(Map.of("type","message","role","assistant","status","completed",
                    "content",List.of(Map.of("type","output_text","text",EvaluationJson.write(assessment(input,verdict))))))));
    }
    public static SecurityTest test() {
        return new SecurityTest(UUID.randomUUID(),"LLM02-CANARY-001",1,"Prueba",com.llmscannerseguridad.domain.securitytest.SecurityCategory.LLM02,"Prompt público");
    }
}
