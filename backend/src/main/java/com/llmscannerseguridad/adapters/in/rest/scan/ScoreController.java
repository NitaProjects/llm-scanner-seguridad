package com.llmscannerseguridad.adapters.in.rest.scan;

import com.llmscannerseguridad.application.scoring.GetScanScoreUseCase;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value="/api/projects/{projectId}/scans/{scanId}/score",produces=MediaType.APPLICATION_JSON_VALUE)
public class ScoreController {
    private final GetScanScoreUseCase scores;
    public ScoreController(GetScanScoreUseCase scores) { this.scores = scores; }
    @GetMapping
    public ResponseEntity<ScoreResponse> score(@PathVariable("projectId") String projectId, @PathVariable("scanId") String scanId) {
        var project = ScanController.identifier(projectId, InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        var scan = ScanController.identifier(scanId, InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(ScoreResponse.from(scores.execute(project, scan)));
    }
}
