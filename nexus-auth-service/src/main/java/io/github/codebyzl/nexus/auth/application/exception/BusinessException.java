package io.github.codebyzl.nexus.auth.application.exception;

public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final BusinessErrorType type;

    protected BusinessException(
            String code,
            String message,
            BusinessErrorType type
    ) {
        super(message);
        this.code = code;
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public BusinessErrorType getType() {
        return type;
    }
}