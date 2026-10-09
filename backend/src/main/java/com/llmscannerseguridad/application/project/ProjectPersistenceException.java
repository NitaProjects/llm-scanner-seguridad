package com.llmscannerseguridad.application.project;

public class ProjectPersistenceException extends RuntimeException {

    public ProjectPersistenceException(Throwable cause) {
        super("No se ha podido acceder al almacenamiento de proyectos.", cause);
    }
}
