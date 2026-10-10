package com.llmscannerseguridad.adapters.in.rest.scan;

import com.llmscannerseguridad.application.scan.ScanOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes={EvaluationController.class, ScoreController.class, ReportController.class})
public class EvaluationExceptionHandler {
    private final ScanExceptionHandler messages=new ScanExceptionHandler();
    @ExceptionHandler(InvalidScanRequestException.class)
    public ResponseEntity<ScanApiError> invalid(InvalidScanRequestException error) { return noStore(messages.invalid(error)); }
    @ExceptionHandler(ScanOperationException.class)
    public ResponseEntity<ScanApiError> operation(ScanOperationException error) { return noStore(messages.operation(error)); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ScanApiError> internal() { return noStore(messages.internal()); }
    private ResponseEntity<ScanApiError> noStore(ResponseEntity<ScanApiError> response) {
        return ResponseEntity.status(response.getStatusCode()).header("Cache-Control","no-store").body(response.getBody());
    }
}
