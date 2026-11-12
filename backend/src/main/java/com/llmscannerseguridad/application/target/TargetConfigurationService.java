package com.llmscannerseguridad.application.target;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Supplier;
import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.EncryptedCredential;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import com.llmscannerseguridad.domain.target.TargetValidationException;

public final class TargetConfigurationService {
    private final ProjectRepositoryPort projects;
    private final TargetConfigurationRepositoryPort targets;
    private final ConfigurationValidationPort validation;
    private final CredentialEncryptionPort encryption;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public TargetConfigurationService(ProjectRepositoryPort projects, TargetConfigurationRepositoryPort targets,
            ConfigurationValidationPort validation, CredentialEncryptionPort encryption,
            Clock clock, Supplier<UUID> identifiers) {
        this.projects = projects;
        this.targets = targets;
        this.validation = validation;
        this.encryption = encryption;
        this.clock = clock;
        this.identifiers = identifiers;
    }

    public TargetView configure(UUID projectId, TargetCommand command) {
        validation.validate(command.settings());
        projects.findById(projectId).orElseThrow(ProjectNotFoundException::new);
        if (targets.findByProjectId(projectId).isPresent()) {
            throw new TargetOperationException(TargetOperationException.Reason.TARGET_ALREADY_CONFIGURED);
        }
        EncryptedCredential credential = credential(command, null);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        var target = new TargetConfiguration(identifiers.get(), projectId, command.settings(), credential, now, now);
        targets.insert(target);
        return TargetView.from(target);
    }

    public TargetView update(UUID projectId, TargetCommand command) {
        validation.validate(command.settings());
        projects.findById(projectId).orElseThrow(ProjectNotFoundException::new);
        var previous = targets.findByProjectId(projectId).orElseThrow(() ->
                new TargetOperationException(TargetOperationException.Reason.TARGET_NOT_CONFIGURED));
        if (!command.adapterTypeExplicit() && previous.settings().adapterType() != com.llmscannerseguridad.domain.target.TargetAdapterType.REST_LEGACY) {
            throw new TargetOperationException(TargetOperationException.Reason.TARGET_TYPE_REQUIRED);
        }
        EncryptedCredential credential = credential(command, previous);
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (now.isBefore(previous.updatedAt())) {
            now = previous.updatedAt();
        }
        var target = new TargetConfiguration(previous.id(), projectId, command.settings(), credential,
                previous.createdAt(), now);
        var expectedType = command.adapterTypeExplicit() ? previous.settings().adapterType()
                : com.llmscannerseguridad.domain.target.TargetAdapterType.REST_LEGACY;
        if (!targets.replace(target, expectedType)) {
            throw new TargetOperationException(command.adapterTypeExplicit()
                    ? TargetOperationException.Reason.TARGET_TYPE_CHANGED : TargetOperationException.Reason.TARGET_TYPE_REQUIRED);
        }
        return TargetView.from(target);
    }

    private EncryptedCredential credential(TargetCommand command, TargetConfiguration previous) {
        if (command.settings().authType() == AuthType.NONE) {
            return null;
        }
        if (command.hasNewSecret()) {
            return encryption.encrypt(command.secretForEncryption());
        }
        var settings = command.settings();
        if (previous != null && previous.credential() != null
                && previous.settings().authType() == settings.authType()
                && java.util.Objects.equals(previous.settings().authHeaderName(), settings.authHeaderName())
                && previous.settings().url().sameOrigin(settings.url())) {
            return previous.credential();
        }
        throw TargetValidationException.field("authConfig.secretValue", "REQUIRED");
    }
}
