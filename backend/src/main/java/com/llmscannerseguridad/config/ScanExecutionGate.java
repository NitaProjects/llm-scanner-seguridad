package com.llmscannerseguridad.config;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import com.llmscannerseguridad.application.scan.ScanFailureHandlerPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScanExecutionGate implements ScanFailureHandlerPort {
    private static final Logger LOG = LoggerFactory.getLogger(ScanExecutionGate.class);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private boolean available;

    public <T> T admit(Supplier<T> operation) {
        lock.readLock().lock();
        try {
            if (!available) {
                throw new ScanOperationException(ScanOperationException.Reason.SCAN_EXECUTION_UNAVAILABLE);
            }
            return operation.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void recovered() {
        lock.writeLock().lock();
        try {
            available = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void persistenceFailed(UUID scanId) {
        lock.writeLock().lock();
        try {
            available = false;
        } finally {
            lock.writeLock().unlock();
        }
        LOG.error("scanId={} code=SCAN_EXECUTION_UNAVAILABLE", scanId);
    }
}
