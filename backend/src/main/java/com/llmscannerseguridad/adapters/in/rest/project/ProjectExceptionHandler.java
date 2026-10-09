package com.llmscannerseguridad.adapters.in.rest.project;

import java.util.List;

import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.domain.project.ProjectValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ProjectController.class)
public class ProjectExceptionHandler {

    @ExceptionHandler(ProjectValidationException.class)
    public ResponseEntity<ProjectApiError> validation(ProjectValidationException error) {
        var fields = error.fieldErrors().stream().map(field ->
                new ProjectApiError.FieldError(field.field(), field.code(), field.message())).toList();
        return ResponseEntity.badRequest().body(new ProjectApiError(
                "VALIDATION_ERROR", "Los datos del proyecto no son válidos.", fields));
    }

    @ExceptionHandler({InvalidProjectRequestException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProjectApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "La petición debe ser un objeto JSON con name y description opcional.");
    }

    @ExceptionHandler(InvalidProjectIdException.class)
    public ResponseEntity<ProjectApiError> invalidIdentifier() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PROJECT_ID",
                "El identificador del proyecto debe ser un UUID válido.");
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ProjectApiError> notFound() {
        return error(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "El proyecto no existe.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProjectApiError> internalError() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No se ha podido completar la operación.");
    }

    private ResponseEntity<ProjectApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ProjectApiError(code, message, List.of()));
    }
}
