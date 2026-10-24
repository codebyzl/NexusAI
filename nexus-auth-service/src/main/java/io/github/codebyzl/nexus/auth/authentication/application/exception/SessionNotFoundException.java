package io.github.codebyzl.nexus.auth.authentication.application.exception;

import io.github.codebyzl.nexus.auth.application.exception.BusinessErrorType;
import io.github.codebyzl.nexus.auth.application.exception.BusinessException;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public class SessionNotFoundException extends BusinessException {
    public SessionNotFoundException() {
        super(
                "SESSION_NOT_FOUND",
                "登录会话不存在",
                BusinessErrorType.NOT_FOUND
        );
    }
}
