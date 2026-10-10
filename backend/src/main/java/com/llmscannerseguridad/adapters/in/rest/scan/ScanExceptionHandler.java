package com.llmscannerseguridad.adapters.in.rest.scan;

import java.util.List;
import com.llmscannerseguridad.application.scan.ScanOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ScanController.class)
public class ScanExceptionHandler {
    @ExceptionHandler(InvalidScanRequestException.class)
    public ResponseEntity<ScanApiError> invalid(InvalidScanRequestException error) {
        String message = switch (error.reason()) {
            case INVALID_PROJECT_ID -> "El identificador del proyecto debe ser un UUID válido.";
            case INVALID_SCAN_ID -> "El identificador del scan debe ser un UUID válido.";
            case INVALID_REQUEST -> "La petición de inicio de scan no admite cuerpo.";
        };
        return ResponseEntity.badRequest().body(new ScanApiError(error.reason().name(), message, List.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ScanApiError> invalidBody() {
        return invalid(new InvalidScanRequestException(InvalidScanRequestException.Reason.INVALID_REQUEST));
    }

    @ExceptionHandler(ScanOperationException.class)
    public ResponseEntity<ScanApiError> operation(ScanOperationException error) {
        HttpStatus status = switch (error.reason()) {
            case PROJECT_NOT_FOUND, TARGET_NOT_CONFIGURED, SCAN_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SCAN_ALREADY_ACTIVE -> HttpStatus.CONFLICT;
            case SCAN_DISPATCH_FAILED, SCAN_EXECUTION_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        String message = switch (error.reason()) {
            case PROJECT_NOT_FOUND -> "El proyecto no existe.";
            case TARGET_NOT_CONFIGURED -> "El proyecto no tiene una configuración de target.";
            case SCAN_NOT_FOUND -> "El scan no existe en el proyecto.";
            case SCAN_ALREADY_ACTIVE -> "El proyecto ya tiene un scan activo.";
            case SCAN_DISPATCH_FAILED -> "No se ha podido aceptar el procesamiento del scan.";
            case SCAN_EXECUTION_UNAVAILABLE -> "La ejecución de scans no está disponible.";
            case INTERNAL_ERROR -> "No se ha podido completar la operación.";
        };
        var response = ResponseEntity.status(status);
        if (error.scanId() != null && error.projectId() != null) {
            response.location(ScanController.location(error.projectId(), error.scanId()));
        }
        return response.body(new ScanApiError(error.reason().name(), message, List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ScanApiError> internal() {
        return operation(ScanOperationException.internal());
    }
}
