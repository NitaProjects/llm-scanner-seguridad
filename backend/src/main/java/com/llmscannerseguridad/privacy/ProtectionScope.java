package com.llmscannerseguridad.privacy;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Handle local al worker; no DTO, registro global ni contenido durable. */
public final class ProtectionScope implements AutoCloseable {
    private static final ThreadLocal<ProtectionScope> CURRENT = new ThreadLocal<>();
    private final UUID scanId;
    private final Set<UUID> observationCoverage = new HashSet<>();
    private java.util.function.BooleanSupplier available = () -> true;
    private final Set<String> values = new LinkedHashSet<>();
    private int maxBytes;
    private int bytes;
    private boolean incomplete;
    private boolean closed;

    private ProtectionScope(UUID scanId, int maxBytes) {
        this.scanId = Objects.requireNonNull(scanId); this.maxBytes = Math.min(maxBytes, 524288);
        if (CURRENT.get() != null) throw ProtectedContentInspector.incomplete();
        CURRENT.set(this);
    }
    public static ProtectionScope open(UUID scanId, int maxBytes) { return new ProtectionScope(scanId, maxBytes); }
    public static boolean active() { return CURRENT.get() != null; }
    public static void register(String value) {
        var scope = CURRENT.get();
        if (scope == null || value == null || value.isEmpty() || scope.values.contains(value)) return;
        if (value.length() > scope.maxBytes - scope.bytes) { scope.incomplete = true; return; }
        int size = value.getBytes(StandardCharsets.UTF_8).length;
        if (scope.closed || scope.values.size() >= ProtectedContentInspector.MAX_VALUES || size > scope.maxBytes - scope.bytes) {
            scope.incomplete = true; return;
        }
        scope.values.add(value); scope.bytes += size;
    }
    public static void incomplete() { var scope = CURRENT.get(); if (scope != null) scope.incomplete = true; }
    public static Set<String> values() { var scope = CURRENT.get(); return scope == null ? Set.of() : Set.copyOf(scope.values); }
    public static boolean complete() { var scope = CURRENT.get(); return scope == null || !scope.closed && !scope.incomplete; }
    public static void limitTo(int bytes) {
        var scope = CURRENT.get();
        if (scope != null) { scope.maxBytes = Math.min(scope.maxBytes, bytes); if (scope.bytes > scope.maxBytes) scope.incomplete = true; }
    }
    public static void deadline(java.util.function.BooleanSupplier check) { var scope = CURRENT.get(); if (scope != null) scope.available = check; }
    public static boolean available() { var scope = CURRENT.get(); return !Thread.currentThread().isInterrupted() && (scope == null || scope.available.getAsBoolean()); }
    public static void coverObservations(UUID id) { var scope = CURRENT.get(); if (scope != null && id != null && complete()) scope.observationCoverage.add(id); }
    public static boolean observationsCovered(UUID id) { var scope = CURRENT.get(); return scope != null && scope.observationCoverage.contains(id); }
    public static void requireScan(UUID scanId) {
        var scope = CURRENT.get();
        if (scope != null && !scope.scanId.equals(scanId)) throw ProtectedContentInspector.incomplete();
    }
    @Override public void close() {
        if (closed) return;
        if (CURRENT.get() != this) throw ProtectedContentInspector.incomplete();
        values.clear(); observationCoverage.clear(); available = () -> false; bytes = 0; closed = true;
        CURRENT.remove();
    }
    @Override public String toString() { return "ProtectionScope[efímero]"; }
}
