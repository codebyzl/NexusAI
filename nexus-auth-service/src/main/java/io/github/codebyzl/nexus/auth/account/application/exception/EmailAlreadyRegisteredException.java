package io.github.codebyzl.nexus.auth.account.application.exception;

import io.github.codebyzl.nexus.auth.application.exception.BusinessErrorType;
import io.github.codebyzl.nexus.auth.application.exception.BusinessException;

public class EmailAlreadyRegisteredException extends BusinessException {

    public EmailAlreadyRegisteredException() {
        super(
                "EMAIL_ALREADY_REGISTERED",
                "该邮箱已被注册",
                BusinessErrorType.CONFLICT
        );
    }
}