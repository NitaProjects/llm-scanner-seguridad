package com.llmscannerseguridad.adapters.out.validation.target;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import com.llmscannerseguridad.domain.target.TargetUrl;
import com.llmscannerseguridad.domain.target.TargetValidationException;

public final class LocalTargetUrlPolicy {
    private final Set<String> demoOrigins;

    public LocalTargetUrlPolicy(boolean localProfile, boolean enabled, Set<String> declaredOrigins) {
        var origins = new HashSet<String>();
        if (localProfile && enabled) {
            for (String entry : declaredOrigins) {
                var url = new TargetUrl(entry);
                var declared = URI.create(url.value());
                if (url.metadataAddress() || declared.getRawQuery() != null
                        || (declared.getRawPath() != null && !declared.getRawPath().isEmpty()
                            && !declared.getRawPath().equals("/"))) {
                    throw new IllegalArgumentException("El origen demo declarado no es válido.");
                }
                origins.add(url.origin());
            }
        }
        demoOrigins = Set.copyOf(origins);
    }

    public void validate(TargetUrl url) {
        if (url.metadataAddress() || (url.restricted() && !demoOrigins.contains(url.origin()))) {
            throw TargetValidationException.field("url", "NOT_ALLOWED");
        }
    }
}
