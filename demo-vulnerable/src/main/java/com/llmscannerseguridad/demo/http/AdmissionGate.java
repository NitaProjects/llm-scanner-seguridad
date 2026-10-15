package com.llmscannerseguridad.demo.http;

import java.util.function.LongSupplier;

public final class AdmissionGate {
    private final LongSupplier ticker;
    private boolean busy;
    private boolean admitted;
    private long lastAdmission;

    public AdmissionGate(LongSupplier ticker) { this.ticker = ticker; }

    public synchronized boolean acquire() {
        long now = ticker.getAsLong();
        if (busy || admitted && now - lastAdmission < 100_000_000L) return false;
        busy = true;
        admitted = true;
        lastAdmission = now;
        return true;
    }

    public synchronized void release() { busy = false; }
}
