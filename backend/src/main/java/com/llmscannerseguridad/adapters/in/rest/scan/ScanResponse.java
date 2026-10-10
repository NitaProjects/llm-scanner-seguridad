package com.llmscannerseguridad.adapters.in.rest.scan;

import java.time.Instant;
import java.util.UUID;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public record ScanResponse(UUID id, UUID projectId, ScanStatus status, String createdAt,
        String startedAt, String finishedAt, ScanFailureReason errorReason) {
    public static ScanResponse from(Scan scan) {
        return new ScanResponse(scan.id(), scan.projectId(), scan.status(), text(scan.createdAt()),
                text(scan.startedAt()), text(scan.finishedAt()), scan.errorReason());
    }

    private static String text(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
