package io.github.codebyzl.nexus.auth.authentication.application.exception;

import io.github.codebyzl.nexus.auth.application.exception.BusinessErrorType;
import io.github.codebyzl.nexus.auth.application.exception.BusinessException;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(
                "INVALID_CREDENTIALS",
                "邮箱或密码错误",
                BusinessErrorType.UNAUTHORIZED
        );
    }
}
