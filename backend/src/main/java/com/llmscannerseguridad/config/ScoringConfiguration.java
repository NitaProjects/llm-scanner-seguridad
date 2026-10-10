package com.llmscannerseguridad.config;

import java.time.Clock;
import javax.sql.DataSource;
import com.llmscannerseguridad.adapters.out.persistence.scoring.PostgresScanScoreRepositoryAdapter;
import com.llmscannerseguridad.adapters.out.persistence.scoring.PostgresScoringInputAdapter;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scoring.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ScoringConfiguration {
    @Bean ScanScoreRepositoryPort scanScoreRepositoryPort(DataSource source) { return new PostgresScanScoreRepositoryAdapter(source); }
    @Bean ScoringInputPort scoringInputPort(DataSource source) { return new PostgresScoringInputAdapter(source); }
    @Bean CalculateScanScoreUseCase calculateScanScoreUseCase(ScoringInputPort inputs) { return new CalculateScanScoreUseCase(inputs, Clock.systemUTC()); }
    @Bean GetScanScoreUseCase getScanScoreUseCase(ProjectRepositoryPort projects, ScanScoreRepositoryPort scores) {
        return new GetScanScoreUseCase(projects, scores);
    }
}
