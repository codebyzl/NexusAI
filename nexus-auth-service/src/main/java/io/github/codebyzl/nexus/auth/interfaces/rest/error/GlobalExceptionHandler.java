package io.github.codebyzl.nexus.auth.interfaces.rest.error;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */

import io.github.codebyzl.nexus.auth.application.exception.BusinessException;
import io.github.codebyzl.nexus.auth.authentication.application.exception.LoginRateLimitedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        HttpStatus status = switch (exception.getType()) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS;
        };

        ApiErrorResponse response = new ApiErrorResponse(
                exception.getCode(),
                exception.getMessage()
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);

        if (exception instanceof LoginRateLimitedException limitedException) {
            Duration retryAfter = limitedException.getRetryAfter();

            // 向上取整，例如 1.5 秒返回 2 秒，至少返回 1 秒
            long seconds = Math.max(
                    1,
                    retryAfter.toSeconds() + (retryAfter.getNano() > 0 ? 1 : 0)
            );

            builder.header(
                    HttpHeaders.RETRY_AFTER,
                    Long.toString(seconds)
            );
        }

        return builder.body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("请求参数不合法");

        return new ApiErrorResponse(
                "INVALID_REQUEST",
                message
        );
    }
}