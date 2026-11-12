package com.llmscannerseguridad.adapters.out.execution.securitytest;

import com.llmscannerseguridad.adapters.out.http.target.HttpTargetInvocationAdapter;
import com.llmscannerseguridad.adapters.out.http.target.OpenAiCompatibleCodec;
import com.llmscannerseguridad.application.execution.*;
import com.llmscannerseguridad.domain.target.TargetConfiguration;

public final class OpenAiCompatibleTargetAdapter implements TargetAdapter {
    private final HttpTargetInvocationAdapter transport;
    private final OpenAiCompatibleCodec codec = new OpenAiCompatibleCodec();
    public OpenAiCompatibleTargetAdapter(HttpTargetInvocationAdapter transport) { this.transport = transport; }
    @Override public NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget) {
        return LegacyInvocationResultProjection.fromLegacy(transport.invokeWithResponseExtractor(target, prompt, budget,
                value -> codec.render(target.settings(), value), codec::extract));
    }
}
