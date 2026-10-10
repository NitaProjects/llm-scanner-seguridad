package com.llmscannerseguridad.adapters.out.persistence.execution;

import java.util.UUID;
import com.llmscannerseguridad.domain.securitytest.SecurityCategory;
import com.llmscannerseguridad.domain.securitytest.SecurityTest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_cases")
public class SecurityTestJpaEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 64, updatable = false)
    private String code;
    @Column(nullable = false, updatable = false)
    private int revision;
    @Column(nullable = false, length = 150, updatable = false)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100, updatable = false)
    private SecurityCategory category;
    @Column(name = "payload_template", nullable = false, updatable = false, columnDefinition = "text")
    private String prompt;

    protected SecurityTestJpaEntity() {
    }

    public SecurityTest toDomain() {
        return new SecurityTest(id, code, revision, name, category, prompt);
    }
}
