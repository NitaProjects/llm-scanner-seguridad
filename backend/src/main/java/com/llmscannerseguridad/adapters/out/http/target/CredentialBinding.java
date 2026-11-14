package com.llmscannerseguridad.adapters.out.http.target;

import java.util.Locale;
import java.util.Set;
import com.llmscannerseguridad.domain.target.TargetRules;

/** Referencia declarada en un paso consumidor. Solo contiene nombres, nunca valores. */
record CredentialBinding(InteractionSession.Capture source, Destination destination, String name) {
    enum Destination { HEADER, COOKIE }

    static String headerName(String name) {
        TargetRules.headerName(name, "credentialBindings", false);
        String lower = name.toLowerCase(Locale.ROOT);
        if (Set.of("accept", "accept-encoding", "expect", "proxy-authenticate").contains(lower)) {
            throw new IllegalArgumentException("Header dinámico reservado.");
        }
        return lower;
    }
}
