package com.llmscannerseguridad.demo.scenario;

public enum DemoProfile {
    SAFE, VULNERABLE;

    public static DemoProfile parse(String value) {
        if ("SAFE".equals(value)) return SAFE;
        if ("VULNERABLE".equals(value)) return VULNERABLE;
        throw new IllegalArgumentException("DEMO_CONFIGURATION_INVALID");
    }
}
