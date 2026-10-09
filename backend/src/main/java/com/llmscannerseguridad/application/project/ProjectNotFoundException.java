package com.llmscannerseguridad.application.project;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
        super("El proyecto no existe.");
    }
}
