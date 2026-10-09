package com.llmscannerseguridad.application.target;

import com.llmscannerseguridad.domain.target.TargetSettings;

public interface ConfigurationValidationPort {
    void validate(TargetSettings settings);
}
