package com.llmscannerseguridad.application.execution;

import com.llmscannerseguridad.domain.target.TargetConfiguration;

public final class RoutingTargetAdapter implements TargetAdapter {
    private final TargetAdapter legacy;
    private final TargetAdapter chat;
    public RoutingTargetAdapter(TargetAdapter legacy, TargetAdapter chat) { this.legacy = legacy; this.chat = chat; }
    @Override public NormalizedInteraction interact(TargetConfiguration target, String prompt, ScanBudget budget) {
        return switch (target.settings().adapterType()) {
            case REST_LEGACY -> legacy.interact(target, prompt, budget);
            case OPENAI_COMPATIBLE -> chat.interact(target, prompt, budget);
        };
    }
}
