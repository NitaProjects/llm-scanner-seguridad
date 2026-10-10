package com.llmscannerseguridad.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public record ScanExecutorSettings(int corePoolSize, int maxPoolSize, int queueCapacity) {
    public ScanExecutorSettings {
        if (corePoolSize <= 0 || maxPoolSize < corePoolSize || queueCapacity < 0 || queueCapacity == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("La configuración del executor de scans no es válida.");
        }
    }

    public ThreadPoolTaskExecutor executor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("scan-worker-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }
}
