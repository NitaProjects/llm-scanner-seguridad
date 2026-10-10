package com.llmscannerseguridad.config;

import java.time.Clock;
import java.util.UUID;
import javax.sql.DataSource;
import com.llmscannerseguridad.adapters.out.persistence.reporting.*;
import com.llmscannerseguridad.adapters.out.reporting.LocalReportPresentationAdapter;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.reporting.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReportingConfiguration {
    @Bean SecurityReportRepositoryPort securityReportRepositoryPort(DataSource source) { return new PostgresSecurityReportRepositoryAdapter(source); }
    @Bean ReportInputPort reportInputPort(DataSource source) { return new PostgresReportInputAdapter(source); }
    @Bean ReportPresentationPort reportPresentationPort() { return new LocalReportPresentationAdapter(); }
    @Bean GenerateReportUseCase generateReportUseCase(ReportInputPort inputs, ReportPresentationPort texts) {
        return new GenerateReportUseCase(inputs,texts,Clock.systemUTC(),UUID::randomUUID);
    }
    @Bean GetSecurityReportUseCase getSecurityReportUseCase(ProjectRepositoryPort projects, SecurityReportRepositoryPort reports) {
        return new GetSecurityReportUseCase(projects,reports);
    }
}
