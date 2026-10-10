package com.llmscannerseguridad.application.reporting;

import com.llmscannerseguridad.domain.reporting.SecurityReport;

public record ReportListing(ReportAvailability availability, SecurityReport report) {
    public ReportListing {
        if (availability == null || (availability == ReportAvailability.AVAILABLE) != (report != null)) {
            throw new IllegalArgumentException("Disponibilidad de informe no válida.");
        }
    }
    @Override public String toString() { return "ReportListing[availability=" + availability + "]"; }
}
