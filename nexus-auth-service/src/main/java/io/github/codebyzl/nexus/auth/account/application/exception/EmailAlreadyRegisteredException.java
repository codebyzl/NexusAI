package io.github.codebyzl.nexus.auth.account.application.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("该邮箱已被注册");
    }
}