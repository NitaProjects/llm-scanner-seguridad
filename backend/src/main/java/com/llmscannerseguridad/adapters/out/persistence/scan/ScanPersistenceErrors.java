package com.llmscannerseguridad.adapters.out.persistence.scan;

import com.llmscannerseguridad.application.scan.ScanOperationException;
import org.hibernate.exception.ConstraintViolationException;

public final class ScanPersistenceErrors {
    private ScanPersistenceErrors() {
    }

    public static ScanOperationException translate(RuntimeException error) {
        if (error instanceof ScanOperationException operation) {
            return operation;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "23505".equals(violation.getSQLState())
                    && "uq_scans_active_project".equals(violation.getConstraintName())) {
                return new ScanOperationException(ScanOperationException.Reason.SCAN_ALREADY_ACTIVE);
            }
        }
        return ScanOperationException.internal();
    }
}
