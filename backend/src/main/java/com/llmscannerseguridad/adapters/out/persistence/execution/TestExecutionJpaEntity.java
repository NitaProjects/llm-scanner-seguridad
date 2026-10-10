package com.llmscannerseguridad.adapters.out.persistence.execution;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.execution.ExecutionError;
import com.llmscannerseguridad.domain.execution.ExecutionStatus;
import com.llmscannerseguridad.domain.execution.TestExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tools.jackson.databind.json.JsonMapper;

@Entity
@Table(name = "test_executions")
public class TestExecutionJpaEntity {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Id
    private UUID id;
    @Column(name = "scan_id", nullable = false, updatable = false)
    private UUID scanId;
    @Column(name = "test_case_id", nullable = false, updatable = false)
    private UUID securityTestId;
    @Column(name = "target_configuration_id", nullable = false, updatable = false)
    private UUID targetConfigurationId;
    @Column(name = "target_updated_at", nullable = false, updatable = false, columnDefinition = "timestamp(6) with time zone")
    private Instant targetUpdatedAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExecutionStatus status;
    @Column(name = "started_at", columnDefinition = "timestamp(6) with time zone")
    private Instant startedAt;
    @Column(name = "finished_at", columnDefinition = "timestamp(6) with time zone")
    private Instant finishedAt;
    @Column(name = "http_status")
    private Integer httpStatus;
    @Column(name = "latency_ms")
    private Long latencyMs;
    @Enumerated(EnumType.STRING)
    @Column(name = "error_reason", length = 40)
    private ExecutionError errorReason;
    @Column(name = "request_payload", columnDefinition = "text")
    private String requestBody;
    @Column(name = "response_payload", columnDefinition = "text")
    private String responsePayload;
    @Column(name = "request_redacted", nullable = false)
    private boolean requestRedacted;
    @Column(name = "response_redacted", nullable = false)
    private boolean responseRedacted;

    protected TestExecutionJpaEntity() {
    }

    public TestExecutionJpaEntity(TestExecution value) {
        id = value.id();
        scanId = value.scanId();
        securityTestId = value.securityTestId();
        targetConfigurationId = value.targetConfigurationId();
        targetUpdatedAt = value.targetUpdatedAt();
        replace(value);
    }

    void replace(TestExecution value) {
        status = value.status();
        startedAt = value.startedAt();
        finishedAt = value.finishedAt();
        httpStatus = value.httpStatus();
        latencyMs = value.latencyMs();
        errorReason = value.errorReason();
        requestBody = value.requestBody();
        responsePayload = encodeResponse(value.responseText());
        requestRedacted = value.requestRedacted();
        responseRedacted = value.responseRedacted();
    }

    static String encodeResponse(String text) {
        return text == null ? null : JSON.writeValueAsString(text);
    }

    public TestExecution toDomain() {
        return new TestExecution(id, scanId, securityTestId, targetConfigurationId, targetUpdatedAt, status,
                startedAt, finishedAt, httpStatus, latencyMs, errorReason, requestBody,
                responsePayload == null ? null : JSON.readValue(responsePayload, String.class),
                requestRedacted, responseRedacted);
    }

    @Override
    public String toString() {
        return "TestExecutionJpaEntity[id=" + id + "]";
    }
}
