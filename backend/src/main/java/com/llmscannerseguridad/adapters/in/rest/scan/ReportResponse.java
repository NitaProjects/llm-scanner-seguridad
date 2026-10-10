package com.llmscannerseguridad.adapters.in.rest.scan;

import com.llmscannerseguridad.application.reporting.*;
import com.llmscannerseguridad.domain.reporting.SecurityReport;

public record ReportResponse(ReportAvailability availability, SecurityReport report) {
    public static ReportResponse from(ReportListing value) { return new ReportResponse(value.availability(),value.report()); }
    @Override public String toString() { return "ReportResponse[availability=" + availability + "]"; }
}
