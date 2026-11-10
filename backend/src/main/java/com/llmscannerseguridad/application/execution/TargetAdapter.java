package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.target.TargetConfiguration;

public interface TargetAdapter {
    NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget);
}
