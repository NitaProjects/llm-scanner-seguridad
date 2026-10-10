package com.llmscannerseguridad.application.execution;

import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.scan.GetScanUseCase;

public final class ListTestExecutionsUseCase {
    private final GetScanUseCase scans;
    private final TestExecutionRepositoryPort executions;

    public ListTestExecutionsUseCase(GetScanUseCase scans, TestExecutionRepositoryPort executions) {
        this.scans = scans;
        this.executions = executions;
    }

    public List<ExecutionMetadata> execute(UUID projectId, UUID scanId) {
        scans.execute(projectId, scanId);
        return executions.listMetadata(scanId);
    }
}
