package com.llmscannerseguridad.adapters.out.http.target;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import com.llmscannerseguridad.domain.execution.ExecutionError;

/** Cuotas acumuladas de una única interacción. El transporte sigue aplicando sus cotas por exchange. */
final class WorkflowBudget {
    private int requestRemaining;
    private int responseRemaining;

    WorkflowBudget(int requestBytes, int responseBytes) {
        requestRemaining = requestBytes;
        responseRemaining = responseBytes;
    }

    void chargeRequest(int bytes) {
        if (bytes > requestRemaining) throw new InvocationFailure(ExecutionError.REQUEST_TOO_LARGE);
        requestRemaining -= bytes;
    }

    int remainingResponse() { return responseRemaining; }

    InputStream countResponse(InputStream input, HttpTargetInvocationAdapter.StepMetrics metrics) {
        return new FilterInputStream(input) {
            @Override public int read() throws IOException {
                int value = in.read();
                if (value >= 0) charge(1);
                return value;
            }

            @Override public int read(byte[] buffer, int offset, int length) throws IOException {
                int count = in.read(buffer, offset, Math.min(length, responseRemaining + 1));
                if (count > 0) charge(count);
                return count;
            }

            private void charge(int count) {
                metrics.bytes += count;
                if (count > responseRemaining) throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
                responseRemaining -= count;
            }
        };
    }
}
