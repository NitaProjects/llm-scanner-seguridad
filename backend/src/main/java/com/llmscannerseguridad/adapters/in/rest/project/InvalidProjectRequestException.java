package com.llmscannerseguridad.adapters.in.rest.project;

public class InvalidProjectRequestException extends RuntimeException {

    public InvalidProjectRequestException() {
        super("La petición debe ser un objeto JSON con name y description opcional.");
    }
}
