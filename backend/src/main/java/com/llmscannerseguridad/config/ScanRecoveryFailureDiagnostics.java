package com.llmscannerseguridad.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.context.ConfigurableApplicationContext;

/** Diagnóstico exclusivo del fallo de recuperación inicial; no altera otros fallos de arranque. */
public final class ScanRecoveryFailureDiagnostics implements SpringBootExceptionReporter {
    private static final String CODE = "SCAN_RECOVERY_FAILED";
    private static final String MESSAGE = CODE + ": No se pudo confirmar la recuperación de scans. "
            + "Backend detenido; revise la disponibilidad de persistencia antes de reiniciar.";

    public ScanRecoveryFailureDiagnostics(ConfigurableApplicationContext context) {
        // Firma requerida por SpringApplication para sus reporters.
    }

    static RuntimeException failure() {
        var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        var filter = new RecoveryLogFilter();
        filter.start();
        context.addTurboFilter(filter);
        // Se descarta la causa técnica antes de que Spring construya excepciones de beans.
        return new RecoveryFailure(context, filter);
    }

    @Override
    public boolean reportException(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof RecoveryFailure recovery) {
                try {
                    LoggerFactory.getLogger(ScanStartup.class).error(MESSAGE);
                } finally {
                    recovery.context.getTurboFilterList().remove(recovery.filter);
                    recovery.filter.stop();
                }
                return true;
            }
        }
        return false;
    }

    private static final class RecoveryFailure extends RuntimeException {
        private final LoggerContext context;
        private final RecoveryLogFilter filter;

        private RecoveryFailure(LoggerContext context, RecoveryLogFilter filter) {
            super(CODE, null, false, false);
            this.context = context;
            this.filter = filter;
        }
    }

    private static final class RecoveryLogFilter extends TurboFilter {
        @Override
        public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                Object[] arguments, Throwable failure) {
            // refresh() concatena BeanCreationException antes de emitir ApplicationFailedEvent.
            // Se suprime solo esa cancelación identificada; los demás logs siguen intactos.
            if (format != null && format.startsWith("Exception encountered during context initialization")
                    && format.contains(CODE)) {
                return FilterReply.DENY;
            }
            return FilterReply.NEUTRAL;
        }
    }
}
