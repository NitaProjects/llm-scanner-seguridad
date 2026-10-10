package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.target.TargetConfiguration;

public interface TargetInvocationPort {
    InvocationResult invoke(TargetConfiguration target, String prompt, ScanBudget budget);
}
