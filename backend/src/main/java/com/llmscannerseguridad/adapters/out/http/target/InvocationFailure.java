package com.llmscannerseguridad.adapters.out.http.target;

import com.llmscannerseguridad.domain.execution.ExecutionError;

final class InvocationFailure extends RuntimeException {
    final ExecutionError reason;

    InvocationFailure(ExecutionError reason) {
        super("La invocación del target no se ha completado.", null, false, false);
        this.reason = reason;
    }
}
