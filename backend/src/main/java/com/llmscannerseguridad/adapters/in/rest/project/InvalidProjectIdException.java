package com.llmscannerseguridad.adapters.in.rest.project;

public class InvalidProjectIdException extends RuntimeException {

    public InvalidProjectIdException() {
        super("El identificador del proyecto debe ser un UUID válido.");
    }
}
