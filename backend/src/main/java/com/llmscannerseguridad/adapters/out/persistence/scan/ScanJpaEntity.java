package com.llmscannerseguridad.adapters.out.persistence.scan;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scans")
public class ScanJpaEntity {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(name = "project_id", nullable = false, updatable = false)
    private UUID projectId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanStatus status;
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamp(6) with time zone")
    private Instant createdAt;
    @Column(name = "started_at", columnDefinition = "timestamp(6) with time zone")
    private Instant startedAt;
    @Column(name = "finished_at", columnDefinition = "timestamp(6) with time zone")
    private Instant finishedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "error_reason", length = 40)
    private ScanFailureReason errorReason;

    protected ScanJpaEntity() {
    }

    public ScanJpaEntity(Scan scan) {
        id = scan.id();
        projectId = scan.projectId();
        status = scan.status();
        createdAt = scan.createdAt();
        startedAt = scan.startedAt();
        finishedAt = scan.finishedAt();
        errorReason = scan.errorReason();
    }

    public Scan toDomain() {
        return new Scan(id, projectId, status, createdAt, startedAt, finishedAt, errorReason);
    }
}
