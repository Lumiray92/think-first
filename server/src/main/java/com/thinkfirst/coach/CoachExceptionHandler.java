package com.thinkfirst.coach;

import com.thinkfirst.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CoachExceptionHandler {

    @ExceptionHandler(CoachSessionNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(CoachSessionNotFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.error(404, e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
    }
}
