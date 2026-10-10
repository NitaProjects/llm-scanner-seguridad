package com.llmscannerseguridad.adapters.out.execution.scan;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import com.llmscannerseguridad.config.ScanConfiguration;
import com.llmscannerseguridad.config.ScanExecutorSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScanExecutorTest {
    @Test
    @Timeout(15)
    void differentBoundedConfigurationsRejectImmediatelyAndNeverRunOnCaller() throws Exception {
        for (var settings : new ScanExecutorSettings[] {new ScanExecutorSettings(1, 1, 1), new ScanExecutorSettings(2, 3, 1)}) {
            var executor = settings.executor();
            executor.initialize();
            var started = new CountDownLatch(settings.maxPoolSize());
            var release = new CountDownLatch(1);
            var finished = new CountDownLatch(settings.maxPoolSize() + settings.queueCapacity());
            var threads = new ConcurrentLinkedQueue<Thread>();
            Thread caller = Thread.currentThread();
            var dispatcher = new SpringScanDispatcherAdapter(executor, scanId -> {
                threads.add(Thread.currentThread());
                started.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Barrera de pruebas no liberada.");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Prueba interrumpida.");
                } finally {
                    finished.countDown();
                }
            });
            try {
                for (int index = 0; index < settings.maxPoolSize() + settings.queueCapacity(); index++) {
                    dispatcher.dispatch(UUID.randomUUID());
                }
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThat(executor.getCorePoolSize()).isEqualTo(settings.corePoolSize());
                assertThat(executor.getMaxPoolSize()).isEqualTo(settings.maxPoolSize());
                assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(settings.queueCapacity());
                assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler()).isInstanceOf(ThreadPoolExecutor.AbortPolicy.class);
                assertThatThrownBy(() -> dispatcher.dispatch(UUID.randomUUID()))
                        .isInstanceOfSatisfying(ScanOperationException.class,
                                error -> assertThat(error.reason()).isEqualTo(ScanOperationException.Reason.SCAN_DISPATCH_FAILED));
                assertThat(threads).doesNotContain(caller);
                assertThat(threads).allSatisfy(thread -> assertThat(thread.getName()).startsWith("scan-worker-"));
                release.countDown();
                assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                release.countDown();
                executor.shutdown();
            }
        }
    }

    @Test
    void settingsAreExternallyBoundAndInvalidValuesFailWithoutReflectingInput() {
        var environment = new MockEnvironment().withProperty("app.scan.executor.core-pool-size", "2")
                .withProperty("app.scan.executor.max-pool-size", "4").withProperty("app.scan.executor.queue-capacity", "0");
        assertThat(new ScanConfiguration().scanExecutorSettings(environment)).isEqualTo(new ScanExecutorSettings(2, 4, 0));
        environment.setProperty("app.scan.executor.queue-capacity", "MARCADOR_CONFIG_PRIVADA");
        assertThatThrownBy(() -> new ScanConfiguration().scanExecutorSettings(environment))
                .hasMessage("La configuración del executor de scans no es válida.").hasNoCause();
        for (int[] values : new int[][] {{0, 1, 1}, {2, 1, 1}, {1, 1, -1}, {1, 1, Integer.MAX_VALUE}}) {
            assertThatThrownBy(() -> new ScanExecutorSettings(values[0], values[1], values[2])).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
