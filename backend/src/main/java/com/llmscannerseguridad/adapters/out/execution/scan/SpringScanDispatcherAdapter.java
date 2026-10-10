package com.llmscannerseguridad.adapters.out.execution.scan;

import java.util.UUID;
import java.util.function.Consumer;
import com.llmscannerseguridad.application.scan.ScanDispatcherPort;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public final class SpringScanDispatcherAdapter implements ScanDispatcherPort {
    private final ThreadPoolTaskExecutor executor;
    private final Consumer<UUID> worker;

    public SpringScanDispatcherAdapter(ThreadPoolTaskExecutor executor, Consumer<UUID> worker) {
        this.executor = executor;
        this.worker = worker;
    }

    @Override
    public void dispatch(UUID scanId) {
        try {
            executor.execute(() -> worker.accept(scanId));
        } catch (RuntimeException error) {
            throw new ScanOperationException(ScanOperationException.Reason.SCAN_DISPATCH_FAILED);
        }
    }
}
