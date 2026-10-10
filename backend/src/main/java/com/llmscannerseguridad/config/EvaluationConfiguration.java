package com.llmscannerseguridad.config;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import javax.sql.DataSource;
import com.llmscannerseguridad.adapters.out.evaluation.*;
import com.llmscannerseguridad.adapters.out.persistence.evaluation.PostgresEvaluationRepositoryAdapter;
import com.llmscannerseguridad.application.evaluation.*;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
public class EvaluationConfiguration {
    @Bean EvaluatorSettings evaluatorSettings(Environment env) {
        try {
            return new EvaluatorSettings(env.getProperty("EVALUATOR_PROVIDER", "OPENAI"), env.getProperty("EVALUATOR_MODEL", "gpt-6-luna"),
                    integer(env,"EVALUATOR_EVIDENCE_BYTES",16384), integer(env,"EVALUATOR_INPUT_BYTES",24576),
                    integer(env,"EVALUATOR_RESPONSE_BYTES",16384), integer(env,"EVALUATOR_MAX_OUTPUT_TOKENS",1024),
                    integer(env,"EVALUATOR_TIMEOUT_MS",20000), integer(env,"EVALUATOR_MAX_CALLS",4), integer(env,"EVALUATOR_INTERVAL_MS",1000),
                    integer(env,"EVALUATOR_RECORD_BYTES",65536), decimal(env,"EVALUATOR_MAX_ESTIMATED_COST_USD"),
                    decimal(env,"EVALUATOR_ESTIMATED_MAX_CALL_COST_USD"), env.getProperty("EVALUATOR_PRICING_PROVIDER"),
                    env.getProperty("EVALUATOR_PRICING_MODEL"), env.getProperty("EVALUATOR_PRICING_VERSION"), env.getProperty("EVALUATOR_ESTIMATE_VERSION"),
                    env.getProperty("EVALUATOR_PRICING_DATE"), env.getProperty("EVALUATOR_PRICING_CURRENCY"),
                    decimal(env,"EVALUATOR_INPUT_PRICE_PER_MILLION"), decimal(env,"EVALUATOR_OUTPUT_PRICE_PER_MILLION"));
        } catch (RuntimeException error) { throw new IllegalArgumentException("La configuración del evaluador no es válida."); }
    }
    private int integer(Environment env,String key,int fallback) { return env.getProperty(key,Integer.class,fallback); }
    private BigDecimal decimal(Environment env,String key) {
        var value=env.getProperty(key); return value==null||value.isBlank()?null:new BigDecimal(value);
    }
    @Bean EvaluatorMessages evaluatorMessages() { return new EvaluatorMessages(); }
    @Bean EvaluationCriteriaCatalogPort evaluationCriteriaCatalogPort() { return new ResourceEvaluationCriteriaCatalog(); }
    @Bean EvaluationScenarioPort evaluationScenarioPort(Environment env,EvaluatorSettings settings) {
        String file=env.getProperty("EVALUATION_SCENARIO_FILE");
        return new LocalEvaluationScenarioAdapter(file==null||file.isBlank()?null:Path.of(file),
                env.getProperty("EVALUATOR_EXPORT_ENABLED",Boolean.class,false),settings.provider(),
                Arrays.stream(env.getProperty("EVALUATOR_ALLOWED_SCENARIOS","").split(",")).map(String::strip)
                    .filter(s->!s.isEmpty()).collect(java.util.stream.Collectors.toSet()));
    }
    @Bean EvaluationEvidenceProjector evaluationEvidenceProjector(EvaluatorSettings settings,EvaluatorMessages messages) {
        return new EvaluationEvidenceProjector(settings,messages,Clock.systemUTC());
    }
    @Bean EvaluationRepositoryPort evaluationRepositoryPort(DataSource source,PlatformTransactionManager manager,
            EvaluationCriteriaCatalogPort catalog,EvaluationScenarioPort scenarios,EvaluationEvidenceProjector projector,EvaluatorSettings settings) {
        return new PostgresEvaluationRepositoryAdapter(source,manager,catalog,scenarios,projector,settings,Clock.systemUTC());
    }
    @Bean(destroyMethod="close") OpenAiEvaluatorAdapter evaluatorPort(EvaluatorSettings settings,EvaluatorMessages messages,Environment env) {
        return new OpenAiEvaluatorAdapter(settings,messages,()->env.getProperty("EVALUATOR_API_KEY"));
    }
    @Bean EvaluateExecutionUseCase evaluateExecutionUseCase(EvaluationRepositoryPort repository,EvaluationEvidenceProjector preparation,
            EvaluatorPort evaluator,EvaluatorSettings settings) {
        return new EvaluateExecutionUseCase(repository,preparation,evaluator,Clock.systemUTC(),System::nanoTime,settings.timeoutMs());
    }
    @Bean ListEvaluationsUseCase listEvaluationsUseCase(GetScanUseCase scans,EvaluationRepositoryPort repository) {
        return new ListEvaluationsUseCase(scans,repository);
    }
    @Bean ListScanFindingsUseCase listScanFindingsUseCase(GetScanUseCase scans,EvaluationRepositoryPort repository) {
        return new ListScanFindingsUseCase(scans,repository);
    }
}
