package com.llmscannerseguridad.adapters.out.execution.securitytest;

import com.llmscannerseguridad.application.execution.LegacyInvocationResultProjection;
import com.llmscannerseguridad.application.execution.NormalizedInteraction;
import com.llmscannerseguridad.application.execution.ScanBudget;
import com.llmscannerseguridad.application.execution.TargetAdapter;
import com.llmscannerseguridad.application.execution.TargetInvocationPort;
import com.llmscannerseguridad.domain.target.TargetConfiguration;

public final class LegacyRestTargetAdapter implements TargetAdapter {
    private final TargetInvocationPort invocation;

    public LegacyRestTargetAdapter(TargetInvocationPort invocation) {
        this.invocation = invocation;
    }

    @Override
    public NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget) {
        return LegacyInvocationResultProjection.fromLegacy(invocation.invoke(target, prompt, budget));
    }
}
