package com.llmscannerseguridad.application.reporting;

import java.util.UUID;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public final class GetSecurityReportUseCase {
    private final ProjectRepositoryPort projects;
    private final SecurityReportRepositoryPort reports;
    public GetSecurityReportUseCase(ProjectRepositoryPort projects, SecurityReportRepositoryPort reports) {
        this.projects = projects; this.reports = reports;
    }
    public ReportListing execute(UUID projectId, UUID scanId) {
        try {
            if (projects.findById(projectId).isEmpty()) throw new ScanOperationException(ScanOperationException.Reason.PROJECT_NOT_FOUND);
            var snapshot = reports.findScoped(projectId,scanId)
                    .orElseThrow(() -> new ScanOperationException(ScanOperationException.Reason.SCAN_NOT_FOUND));
            snapshot.validate();
            if (snapshot.report() != null) return new ReportListing(ReportAvailability.AVAILABLE,snapshot.report());
            return new ReportListing(snapshot.reportVersion() == null ? ReportAvailability.NOT_APPLICABLE
                    : snapshot.scan().status() == ScanStatus.FAILED ? ReportAvailability.UNAVAILABLE : ReportAvailability.PENDING,null);
        } catch (ScanOperationException error) { throw error; }
        catch (RuntimeException error) { throw ScanOperationException.internal(); }
    }
}
