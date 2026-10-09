package com.llmscannerseguridad.adapters.in.rest.target;

public final class InvalidTargetRequestException extends RuntimeException {
    public InvalidTargetRequestException() {
        super("La petición de configuración no es válida.");
    }
}
