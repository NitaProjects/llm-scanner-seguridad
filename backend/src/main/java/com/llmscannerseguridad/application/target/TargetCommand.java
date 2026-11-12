package com.llmscannerseguridad.application.target;

import java.util.Objects;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.TargetRules;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.target.TargetValidationException;

// No es un record: el secreto efímero no debe aparecer en toString.
public final class TargetCommand {
    private final TargetSettings settings;
    private final String secret;
    private final boolean secretPresent;
    private final boolean adapterTypeExplicit;

    public TargetCommand(TargetSettings settings, String secret, boolean secretPresent) {
        this(settings, secret, secretPresent, false);
    }

    public TargetCommand(TargetSettings settings, String secret, boolean secretPresent, boolean adapterTypeExplicit) {
        this.settings = Objects.requireNonNull(settings);
        this.secret = secret;
        this.secretPresent = secretPresent;
        this.adapterTypeExplicit = adapterTypeExplicit;
        if (secretPresent) {
            if (settings.authType() == AuthType.NONE) {
                throw TargetValidationException.field("authConfig", "NOT_ALLOWED");
            }
            TargetRules.secret(secret, settings.authType());
        }
    }

    public TargetSettings settings() {
        return settings;
    }

    public boolean hasNewSecret() {
        return secretPresent;
    }

    public boolean adapterTypeExplicit() { return adapterTypeExplicit; }

    public String secretForEncryption() {
        return secret;
    }

    @Override
    public String toString() {
        return "TargetCommand[credencial protegida]";
    }
}
