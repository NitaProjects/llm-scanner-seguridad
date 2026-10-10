package com.llmscannerseguridad.adapters.out.http.target;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.llmscannerseguridad.domain.execution.ExecutionError;

public final class BoundedDnsResolver implements AutoCloseable {
    @FunctionalInterface
    public interface Lookup {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private final Lookup lookup;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(8), task -> {
                var thread = new Thread(task, "target-dns");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public BoundedDnsResolver(Lookup lookup) {
        this.lookup = lookup;
    }

    public InetAddress[] resolve(String host, long timeoutNanos) {
        java.util.concurrent.Future<InetAddress[]> future = null;
        try {
            if (timeoutNanos <= 0) {
                throw new InvocationFailure(ExecutionError.DNS_RESOLUTION_FAILED);
            }
            if (host.contains(":" ) || host.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+")) {
                return new InetAddress[] {InetAddress.getByName(host)};
            }
            future = executor.submit(() -> lookup.resolve(host));
            return future.get(timeoutNanos, TimeUnit.NANOSECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new InvocationFailure(ExecutionError.DNS_RESOLUTION_FAILED);
        } catch (Exception error) {
            throw new InvocationFailure(ExecutionError.DNS_RESOLUTION_FAILED);
        } finally {
            if (future != null) {
                future.cancel(true);
                executor.purge();
            }
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
