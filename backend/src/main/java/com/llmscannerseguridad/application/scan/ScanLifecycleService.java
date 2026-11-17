package com.llmscannerseguridad.application.scan;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import com.llmscannerseguridad.application.project.ProjectRepositoryPort;
import com.llmscannerseguridad.application.target.TargetConfigurationRepositoryPort;
import com.llmscannerseguridad.domain.scan.Scan;
import com.llmscannerseguridad.domain.scan.ScanFailureReason;
import com.llmscannerseguridad.domain.scan.ScanStatus;

public final class ScanLifecycleService implements ScanLifecyclePort {
    private final ProjectRepositoryPort projects;
    private final TargetConfigurationRepositoryPort targets;
    private final ScanRepositoryPort scans;
    private final Clock clock;
    private final Supplier<UUID> identifiers;

    public ScanLifecycleService(ProjectRepositoryPort projects, TargetConfigurationRepositoryPort targets,
            ScanRepositoryPort scans, Clock clock, Supplier<UUID> identifiers) {
        this.projects = projects;
        this.targets = targets;
        this.scans = scans;
        this.clock = clock;
        this.identifiers = identifiers;
    }

    @Override
    public Scan create(UUID projectId) {
        if (!projects.lockForDependentWrite(projectId)) {
            throw new ScanOperationException(ScanOperationException.Reason.PROJECT_NOT_FOUND);
        }
        if (!targets.existsByProjectId(projectId)) {
            throw new ScanOperationException(ScanOperationException.Reason.TARGET_NOT_CONFIGURED);
        }
        if (scans.existsActiveByProjectId(projectId)) {
            throw new ScanOperationException(ScanOperationException.Reason.SCAN_ALREADY_ACTIVE);
        }
        var scan = Scan.pending(identifiers.get(), projectId, clock.instant());
        scans.insert(scan);
        return scan;
    }

    @Override
    public Optional<Scan> claim(UUID scanId) {
        var current = scans.findById(scanId);
        if (current.isEmpty() || current.get().status() != ScanStatus.PENDING) {
            return Optional.empty();
        }
        var running = current.get().start(clock.instant());
        return scans.transition(ScanStatus.PENDING, running) ? Optional.of(running) : Optional.empty();
    }

    @Override
    public boolean complete(UUID scanId) {
        var current = scans.findById(scanId);
        return current.isPresent() && current.get().status() == ScanStatus.RUNNING
                && scans.transition(ScanStatus.RUNNING, current.get().complete(clock.instant()));
    }

    @Override
    public boolean fail(UUID scanId, ScanStatus expected, ScanFailureReason reason) {
        var current = scans.findById(scanId);
        if (current.isEmpty() || current.get().status() != expected || !expected.active()) {
            return false;
        }
        var scan = current.get();
        return scans.transition(scan.status(), scan.fail(reason, clock.instant()));
    }

    @Override
    public int recoverInterrupted() {
        int recovered = 0;
        for (var scan : scans.findActive()) {
            if (!scans.transition(scan.status(), scan.fail(ScanFailureReason.SCAN_INTERRUPTED, clock.instant()))) {
                throw ScanOperationException.internal();
            }
            recovered++;
        }
        return recovered;
    }
}
