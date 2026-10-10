package com.llmscannerseguridad.adapters.in.rest.scan;

import java.util.List;
import com.llmscannerseguridad.application.execution.ExecutionMetadata;
import com.llmscannerseguridad.application.execution.ListTestExecutionsUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/scans/{scanId}/executions", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestExecutionController {
    private final ListTestExecutionsUseCase executions;

    public TestExecutionController(ListTestExecutionsUseCase executions) {
        this.executions = executions;
    }

    @GetMapping
    public ResponseEntity<List<ExecutionMetadata>> list(@PathVariable("projectId") String projectId,
            @PathVariable("scanId") String scanId) {
        var project = ScanController.identifier(projectId, InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        var scan = ScanController.identifier(scanId, InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(executions.execute(project, scan));
    }
}
