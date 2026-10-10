package com.llmscannerseguridad.adapters.in.rest.scan;

import java.net.URI;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import com.llmscannerseguridad.application.scan.GetScanUseCase;
import com.llmscannerseguridad.application.scan.ListProjectScansUseCase;
import com.llmscannerseguridad.application.scan.StartScanUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/projects/{projectId}/scans", produces = MediaType.APPLICATION_JSON_VALUE)
public class ScanController {
    private final StartScanUseCase start;
    private final GetScanUseCase get;
    private final ListProjectScansUseCase list;

    public ScanController(StartScanUseCase start, GetScanUseCase get, ListProjectScansUseCase list) {
        this.start = start;
        this.get = get;
        this.list = list;
    }

    @PostMapping
    public ResponseEntity<ScanResponse> start(@PathVariable("projectId") String projectId,
            HttpServletRequest request) throws IOException {
        UUID project = identifier(projectId, InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        // No depende de Content-Length ni materializa el cuerpo rechazado.
        if (request.getInputStream().read() != -1) {
            throw new InvalidScanRequestException(InvalidScanRequestException.Reason.INVALID_REQUEST);
        }
        var scan = start.execute(project);
        return ResponseEntity.accepted().location(location(project, scan.id())).body(ScanResponse.from(scan));
    }

    @GetMapping("/{scanId}")
    public ResponseEntity<ScanResponse> get(@PathVariable("projectId") String projectId,
            @PathVariable("scanId") String scanId) {
        UUID project = identifier(projectId, InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        UUID scan = identifier(scanId, InvalidScanRequestException.Reason.INVALID_SCAN_ID);
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(ScanResponse.from(get.execute(project, scan)));
    }

    @GetMapping
    public ResponseEntity<List<ScanResponse>> list(@PathVariable("projectId") String projectId) {
        UUID project = identifier(projectId, InvalidScanRequestException.Reason.INVALID_PROJECT_ID);
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(list.execute(project).stream().map(ScanResponse::from).toList());
    }

    static URI location(UUID projectId, UUID scanId) {
        return URI.create("/api/projects/" + projectId + "/scans/" + scanId);
    }

    static UUID identifier(String value, InvalidScanRequestException.Reason reason) {
        if (!value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new InvalidScanRequestException(reason);
        }
        return UUID.fromString(value);
    }
}
