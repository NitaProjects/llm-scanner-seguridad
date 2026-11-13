package com.llmscannerseguridad.adapters.out.http.target;

import java.util.List;
import java.util.Set;
import com.llmscannerseguridad.domain.target.TargetAdapterType;
import com.llmscannerseguridad.domain.target.TargetSettings;

/** Selección operativa ya validada, sin valores de cabeceras. */
record StreamBinding(String mediaType, List<Object> eventPath, List<Object> textPath,
        String contentEvent, String errorEvent, String terminalEvent, String terminalLiteral,
        Mode mode, Set<String> reservedHeaders) {
    enum Mode { DELTA, SNAPSHOT }

    boolean sse() { return mediaType.equals("text/event-stream"); }

    boolean permits(TargetSettings settings) {
        return settings.adapterType() == TargetAdapterType.REST_LEGACY
                && !"Accept".equalsIgnoreCase(settings.authHeaderName())
                && reservedHeaders.stream().noneMatch("Accept"::equalsIgnoreCase);
    }
}
