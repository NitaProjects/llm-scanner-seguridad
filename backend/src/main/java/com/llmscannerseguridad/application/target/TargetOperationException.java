package com.llmscannerseguridad.application.target;

public final class TargetOperationException extends RuntimeException {
    public enum Reason { TARGET_NOT_CONFIGURED, TARGET_ALREADY_CONFIGURED, TARGET_TYPE_REQUIRED, TARGET_TYPE_CHANGED, INTERNAL_ERROR }
    private final Reason reason;

    public TargetOperationException(Reason reason) {
        super("No se ha podido completar la operación de target.");
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public static TargetOperationException internal() {
        return new TargetOperationException(Reason.INTERNAL_ERROR);
    }
}
