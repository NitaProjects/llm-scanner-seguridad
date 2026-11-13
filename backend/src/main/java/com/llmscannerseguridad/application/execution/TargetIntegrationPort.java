package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.target.TargetConfiguration;

public interface TargetIntegrationPort {
    /** Resuelve un adapter fijado para el plan o devuelve null para el flujo habitual. */
    TargetAdapter resolve(TargetConfiguration target);
}
