package com.llmscannerseguridad.adapters.out.persistence.target;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.llmscannerseguridad.domain.target.AuthType;
import com.llmscannerseguridad.domain.target.EncryptedCredential;
import com.llmscannerseguridad.domain.target.TargetConfiguration;
import com.llmscannerseguridad.domain.target.TargetSettings;
import com.llmscannerseguridad.domain.target.TargetUrl;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "target_configurations")
public class TargetConfigurationJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "project_id", nullable = false, unique = true, updatable = false)
    private UUID projectId;
    @Column(length = 120)
    private String name;
    @Column(nullable = false, length = 2048)
    private String url;
    @Column(name = "request_template", nullable = false, columnDefinition = "text")
    private String requestTemplate;
    @Column(name = "response_path", nullable = false, length = 1024)
    private String responsePath;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> headers;
    @Column(name = "auth_type", nullable = false, length = 20)
    private String authType;
    @Column(name = "auth_header_name", length = 128)
    private String authHeaderName;
    @Column(name = "credential_ciphertext")
    private byte[] credentialCiphertext;
    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp(6) with time zone")
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp(6) with time zone")
    private Instant updatedAt;

    protected TargetConfigurationJpaEntity() {
    }

    public TargetConfigurationJpaEntity(TargetConfiguration target) {
        id = target.id();
        projectId = target.projectId();
        createdAt = target.createdAt();
        replace(target);
    }

    public void replace(TargetConfiguration target) {
        var settings = target.settings();
        name = settings.name();
        url = settings.url().value();
        requestTemplate = settings.requestTemplate();
        responsePath = settings.responsePath();
        headers = settings.headers();
        authType = settings.authType().name();
        authHeaderName = settings.authHeaderName();
        credentialCiphertext = target.credential() == null ? null : target.credential().bytes();
        timeoutMs = settings.timeoutMs();
        updatedAt = target.updatedAt();
    }

    public TargetConfiguration toDomain() {
        var settings = new TargetSettings(name, new TargetUrl(url), requestTemplate, responsePath, headers,
                AuthType.valueOf(authType), authHeaderName, timeoutMs);
        return new TargetConfiguration(id, projectId, settings,
                credentialCiphertext == null ? null : new EncryptedCredential(credentialCiphertext),
                createdAt, updatedAt);
    }
}
