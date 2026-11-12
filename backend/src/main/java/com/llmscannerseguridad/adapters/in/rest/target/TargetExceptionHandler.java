package com.llmscannerseguridad.adapters.in.rest.target;

import java.util.List;
import com.llmscannerseguridad.adapters.in.rest.project.InvalidProjectIdException;
import com.llmscannerseguridad.application.project.ProjectNotFoundException;
import com.llmscannerseguridad.application.target.TargetOperationException;
import com.llmscannerseguridad.domain.target.TargetValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = TargetController.class)
public class TargetExceptionHandler {
    @ExceptionHandler(TargetValidationException.class)
    public ResponseEntity<TargetApiError> validation(TargetValidationException error) {
        return ResponseEntity.badRequest().body(new TargetApiError("VALIDATION_ERROR",
                "Los datos del target no son válidos.", error.fieldErrors()));
    }

    @ExceptionHandler({InvalidTargetRequestException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<TargetApiError> request() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "La petición de configuración no es válida.");
    }

    @ExceptionHandler(InvalidProjectIdException.class)
    public ResponseEntity<TargetApiError> identifier() {
        return error(HttpStatus.BAD_REQUEST, "INVALID_PROJECT_ID",
                "El identificador del proyecto debe ser un UUID válido.");
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<TargetApiError> project() {
        return error(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "El proyecto no existe.");
    }

    @ExceptionHandler(TargetOperationException.class)
    public ResponseEntity<TargetApiError> operation(TargetOperationException exception) {
        return switch (exception.reason()) {
            case TARGET_NOT_CONFIGURED -> error(HttpStatus.NOT_FOUND, "TARGET_NOT_CONFIGURED",
                    "El proyecto no tiene target configurado.");
            case TARGET_ALREADY_CONFIGURED -> error(HttpStatus.CONFLICT, "TARGET_ALREADY_CONFIGURED",
                    "El proyecto ya tiene target configurado.");
            case TARGET_TYPE_REQUIRED -> error(HttpStatus.CONFLICT, "TARGET_TYPE_REQUIRED", "Consulta el target y selecciona explícitamente su tipo.");
            case TARGET_TYPE_CHANGED -> error(HttpStatus.CONFLICT, "TARGET_TYPE_CHANGED", "El tipo del target cambió. Consulta su configuración antes de guardar.");
            case INTERNAL_ERROR -> internal();
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<TargetApiError> internal() {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No se ha podido completar la operación.");
    }

    private ResponseEntity<TargetApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new TargetApiError(code, message, List.of()));
    }
}
