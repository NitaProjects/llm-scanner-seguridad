package com.llmscannerseguridad.application.execution;

public final class LegacyInvocationResultProjection {
    private LegacyInvocationResultProjection() { }

    public static NormalizedInteraction fromLegacy(InvocationResult result) {
        if (result == null) return null;
        return new NormalizedInteraction(result.responseText(), result.latencyMs(), result.errorReason(),
                result.requestRedacted(), result.responseRedacted(),
                new NormalizedInteraction.LegacyCompatibility(result.httpStatus(), result.requestBody()));
    }

    public static InvocationResult toLegacy(NormalizedInteraction interaction) {
        if (interaction == null) return null;
        return new InvocationResult(interaction.legacy().httpStatus(), interaction.latencyMs(), interaction.errorReason(),
                interaction.legacy().requestBody(), interaction.responseText(),
                interaction.requestRedacted(), interaction.responseRedacted());
    }
}
