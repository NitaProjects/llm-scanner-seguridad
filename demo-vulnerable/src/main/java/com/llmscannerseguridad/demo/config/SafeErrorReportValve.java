package com.llmscannerseguridad.demo.config;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

// Errores previos al filtro (p.ej. cabeceras): sin URL, valores ni stacktrace.
public final class SafeErrorReportValve extends ErrorReportValve {
    public SafeErrorReportValve() {
        setShowReport(false);
        setShowServerInfo(false);
    }

    @Override
    protected void report(Request request, Response response, Throwable error) {
        if (response.getStatus() < 400 || response.isCommitted() || !response.setErrorReported()) return;
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("X-Content-Type-Options", "nosniff");
            String code = response.getStatus() == 408 ? "REQUEST_TIMEOUT"
                    : response.getStatus() >= 500 ? "INTERNAL_ERROR" : "INVALID_REQUEST";
            var writer = response.getReporter();
            if (writer != null) writer.write("{\"code\":\"" + code + "\"}");
            response.finishResponse();
        } catch (java.io.IOException ignored) {
            // El conector puede haber cerrado el socket.
        }
    }
}
