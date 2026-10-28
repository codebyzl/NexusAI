package io.github.codebyzl.nexus.auth.authentication.application.exception;

import io.github.codebyzl.nexus.auth.application.exception.BusinessErrorType;
import io.github.codebyzl.nexus.auth.application.exception.BusinessException;

import java.time.Duration;

public class LoginRateLimitedException extends BusinessException {

    private final Duration retryAfter;

    public LoginRateLimitedException(Duration retryAfter) {
        super(
                "LOGIN_RATE_LIMITED",
                "登录尝试过于频繁，请稍后重试",
                BusinessErrorType.TOO_MANY_REQUESTS
        );
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}