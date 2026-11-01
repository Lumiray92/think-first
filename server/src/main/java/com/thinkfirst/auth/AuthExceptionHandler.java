package com.thinkfirst.auth;

import com.thinkfirst.common.ApiResponse;
import com.thinkfirst.vault.ApiKeyException;
import com.thinkfirst.vault.KeyVaultUnavailableException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("请求参数不正确");
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(EmailAlreadyUsedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleEmailUsed(EmailAlreadyUsedException e) {
        return ApiResponse.error(409, e.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidCredentials(InvalidCredentialsException e) {
        return ApiResponse.error(401, e.getMessage());
    }

    @ExceptionHandler(ApiKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleApiKey(ApiKeyException e) {
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(KeyVaultUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleVault(KeyVaultUnavailableException e) {
        return ApiResponse.error(503, e.getMessage());
    }
}
