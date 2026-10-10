package com.llmscannerseguridad.adapters.in.rest.scan;

import com.llmscannerseguridad.adapters.reporting.ReportJson;
import com.llmscannerseguridad.application.reporting.GetSecurityReportUseCase;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/projects/{projectId}/scans/{scanId}/report",produces=MediaType.APPLICATION_JSON_VALUE)
public class ReportController {
    private final GetSecurityReportUseCase reports;
    public ReportController(GetSecurityReportUseCase reports) { this.reports = reports; }
    @GetMapping
    public ResponseEntity<String> report(@PathVariable("projectId") String projectId, @PathVariable("scanId") String scanId) {
        var project = ScanController.identifier(projectId,InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        var scan = ScanController.identifier(scanId,InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        var response = ReportResponse.from(reports.execute(project,scan));
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).header("Cache-Control","no-store")
                .body(ReportJson.response(response,response.report()));
    }
}
