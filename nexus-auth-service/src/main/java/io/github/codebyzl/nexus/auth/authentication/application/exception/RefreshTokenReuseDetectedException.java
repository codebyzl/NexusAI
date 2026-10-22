package io.github.codebyzl.nexus.auth.authentication.application.exception;

import io.github.codebyzl.nexus.auth.application.exception.BusinessErrorType;
import io.github.codebyzl.nexus.auth.application.exception.BusinessException;

public class RefreshTokenReuseDetectedException extends BusinessException {

    public RefreshTokenReuseDetectedException() {
        super(
                "INVALID_REFRESH_TOKEN",
                "Refresh Token 无效或已过期",
                BusinessErrorType.UNAUTHORIZED
        );
    }
}
