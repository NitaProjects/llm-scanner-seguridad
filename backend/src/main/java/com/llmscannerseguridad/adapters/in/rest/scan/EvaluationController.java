package com.llmscannerseguridad.adapters.in.rest.scan;

import java.util.List;
import com.llmscannerseguridad.application.evaluation.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/projects/{projectId}/scans/{scanId}",produces=MediaType.APPLICATION_JSON_VALUE)
public class EvaluationController {
    private final ListEvaluationsUseCase evaluations;
    private final ListScanFindingsUseCase findings;
    public EvaluationController(ListEvaluationsUseCase evaluations,ListScanFindingsUseCase findings) {
        this.evaluations=evaluations; this.findings=findings;
    }
    @GetMapping("/evaluations")
    public ResponseEntity<EvaluationListing> evaluations(@PathVariable("projectId") String projectId,@PathVariable("scanId") String scanId) {
        var project=ScanController.identifier(projectId,InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        var scan=ScanController.identifier(scanId,InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(evaluations.execute(project,scan));
    }
    @GetMapping("/findings")
    public ResponseEntity<List<FindingMetadata>> findings(@PathVariable("projectId") String projectId,@PathVariable("scanId") String scanId) {
        var project=ScanController.identifier(projectId,InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        var scan=ScanController.identifier(scanId,InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(findings.execute(project,scan));
    }
}
