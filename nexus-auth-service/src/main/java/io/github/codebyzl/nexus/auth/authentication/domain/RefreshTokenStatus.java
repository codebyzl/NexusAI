package io.github.codebyzl.nexus.auth.authentication.domain;

public enum RefreshTokenStatus {

    ACTIVE(1),
    USED(2),
    REVOKED(3);

    private final int code;

    RefreshTokenStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static RefreshTokenStatus fromCode(int code) {
        for (RefreshTokenStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException(
                "Unknown refresh token status: " + code
        );
    }
}
