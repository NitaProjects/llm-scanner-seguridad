package com.llmscannerseguridad.demo.http;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import com.llmscannerseguridad.demo.scenario.DemoProfile;
import com.llmscannerseguridad.demo.scenario.ScenarioCatalog;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

// Frontera HTTP única: no delega errores a páginas HTML ni al binding permisivo de MVC.
public final class DemoHttpFilter implements Filter {
    private final ScenarioCatalog catalog;
    private final DemoProfile profile;
    private final AdmissionGate gate;

    public DemoHttpFilter(ScenarioCatalog catalog, DemoProfile profile, AdmissionGate gate) {
        this.catalog = catalog;
        this.profile = profile;
        this.gate = gate;
    }

    @Override
    public void doFilter(ServletRequest input, ServletResponse output, FilterChain chain) throws IOException {
        var request = (HttpServletRequest) input;
        var response = (HttpServletResponse) output;
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        boolean admitted = false;
        try {
            if (request.getDispatcherType() == jakarta.servlet.DispatcherType.ERROR) {
                Object original = request.getAttribute(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE);
                int status = original instanceof Integer number ? number : 500;
                String code = status == 408 ? "REQUEST_TIMEOUT" : status == 404 ? "NOT_FOUND"
                        : status >= 500 ? "INTERNAL_ERROR" : "INVALID_REQUEST";
                write(response, status, Map.of("code", code));
                return;
            }
            String path = request.getRequestURI();
            if (!path.equals("/api/chat") && !path.equals("/health")) throw new RequestFailure(404, "NOT_FOUND");
            String method = path.equals("/api/chat") ? "POST" : "GET";
            if (!method.equals(request.getMethod())) {
                response.setHeader("Allow", method);
                throw new RequestFailure(405, "METHOD_NOT_ALLOWED");
            }
            if (request.getQueryString() != null) throw new RequestFailure(400, "INVALID_REQUEST");
            if (path.equals("/health")) {
                write(response, 200, Map.of("status", "UP", "demo", true, "profile", profile.name(),
                        "scenarioVersion", ScenarioCatalog.VERSION));
                return;
            }
            mediaType(request);
            if (!gate.acquire()) throw new RequestFailure(429, "RATE_LIMITED");
            admitted = true;
            if (request.getContentLengthLong() > DemoJson.BODY_BYTES) throw new RequestFailure(413, "REQUEST_TOO_LARGE");
            String message = DemoJson.message(DemoJson.read(request.getInputStream(), DemoJson.BODY_BYTES));
            write(response, 200, Map.of("response", catalog.respond(message)));
        } catch (RequestFailure error) {
            fail(response, error.status(), error.code());
        } catch (IOException error) {
            fail(response, timeout(error) ? 408 : 400, timeout(error) ? "REQUEST_TIMEOUT" : "INVALID_REQUEST");
        } catch (RuntimeException error) {
            fail(response, 500, "INTERNAL_ERROR");
        } finally {
            if (admitted) gate.release();
        }
    }

    private void mediaType(HttpServletRequest request) {
        try {
            var types = Collections.list(request.getHeaders("Content-Type"));
            if (types.size() != 1) throw new IllegalArgumentException();
            var type = MediaType.parseMediaType(types.getFirst());
            if (!type.getType().equalsIgnoreCase("application") || !type.getSubtype().equalsIgnoreCase("json")
                    || type.getCharset() != null && !StandardCharsets.UTF_8.equals(type.getCharset())
                    || type.getParameters().keySet().stream().anyMatch(key -> !key.equalsIgnoreCase("charset"))) {
                throw new IllegalArgumentException();
            }
            for (String encoding : Collections.list(request.getHeaders("Content-Encoding"))) {
                if (!encoding.equalsIgnoreCase("identity")) throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            throw new RequestFailure(415, "UNSUPPORTED_MEDIA_TYPE");
        }
    }

    private boolean timeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) return true;
        }
        return false;
    }

    private void write(HttpServletResponse response, int status, Object value) throws IOException {
        byte[] bytes = DemoJson.response(value);
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
    }

    private void fail(HttpServletResponse response, int status, String code) {
        if (response.isCommitted()) return;
        try {
            response.resetBuffer();
            response.setHeader("Connection", "close"); // No consumir el resto de un body rechazado.
            write(response, status, Map.of("code", code));
        } catch (IOException ignored) {
            // Socket cerrado/timeout: no registrar el contenido ni la causa.
        }
    }
}
