package com.llmscannerseguridad.adapters.out.http.target;

@FunctionalInterface
public interface TargetResponseTextExtractor {
    String extract(String body);
}
