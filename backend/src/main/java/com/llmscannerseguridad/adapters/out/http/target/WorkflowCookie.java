package com.llmscannerseguridad.adapters.out.http.target;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.target.TargetRules;
import org.apache.hc.client5.http.utils.DateUtils;

record WorkflowCookie(String value, String path, Instant expires, boolean secure, boolean httpOnly, String sameSite) {
    static WorkflowCookie parse(String header, String name, URI source, Instant now, int maxBytes) {
        String[] parts = header.split(";", -1);
        int separator = parts[0].indexOf('=');
        if (separator <= 0 || !parts[0].substring(0, separator).strip().equals(name)) return null;
        if (parts.length > 17) throw malformed();
        var attributes = new HashMap<String, String>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].strip();
            int equals = part.indexOf('=');
            String key = (equals < 0 ? part : part.substring(0, equals)).toLowerCase(Locale.ROOT);
            String data = equals < 0 ? "" : part.substring(equals + 1).strip();
            if (!key.matches("[a-z][a-z0-9-]{0,31}") || data.length() > 1024 || TargetRules.controls(data)
                    || attributes.putIfAbsent(key, data) != null) throw malformed();
        }
        if (attributes.containsKey("domain")) throw malformed();
        String path = attributes.get("path");
        if (path == null || !path.startsWith("/")) {
            String requestPath = source.getRawPath();
            int slash = requestPath.lastIndexOf('/');
            path = slash <= 0 ? "/" : requestPath.substring(0, slash);
        }
        Instant expires = null;
        String maxAge = attributes.get("max-age");
        if (maxAge != null) {
            if (!maxAge.matches("-?[0-9]+")) throw malformed();
            var seconds = new java.math.BigInteger(maxAge);
            expires = seconds.signum() <= 0 ? now : now.plusSeconds(seconds.min(java.math.BigInteger.valueOf(Integer.MAX_VALUE)).longValue());
        } else if (attributes.containsKey("expires")) {
            expires = DateUtils.parseStandardDate(attributes.get("expires"));
            if (expires == null) throw malformed();
        }
        String value = parts[0].substring(separator + 1).strip();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
        if (!value.matches("[\\x21\\x23-\\x2B\\x2D-\\x3A\\x3C-\\x5B\\x5D-\\x7E]*")) throw malformed();
        com.llmscannerseguridad.privacy.ProtectionScope.register(value);
        if (TargetRules.bytes(value) > maxBytes) throw new InvocationFailure(ExecutionError.RESPONSE_TOO_LARGE);
        if (value.isEmpty() && (expires == null || expires.isAfter(now))) throw malformed();
        boolean secure = attributes.containsKey("secure");
        if (secure && !source.getScheme().equalsIgnoreCase("https")) throw malformed();
        return new WorkflowCookie(value, path, expires, secure, attributes.containsKey("httponly"), attributes.get("samesite"));
    }

    boolean applies(URI destination, Instant now) {
        String requestPath = destination.getRawPath();
        if (requestPath.isEmpty()) requestPath = "/";
        return (expires == null || expires.isAfter(now))
                && (!secure || destination.getScheme().equalsIgnoreCase("https"))
                && (requestPath.equals(path) || requestPath.startsWith(path.endsWith("/") ? path : path + "/"));
    }

    @Override public String toString() { return "WorkflowCookie[protegida]"; }

    private static InvocationFailure malformed() {
        com.llmscannerseguridad.privacy.ProtectionScope.incomplete();
        return new InvocationFailure(ExecutionError.RESPONSE_FORMAT_ERROR);
    }
}
