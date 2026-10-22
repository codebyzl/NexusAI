package io.github.codebyzl.nexus.auth.authentication.application;

public record RotateRefreshTokenCommand(
        String refreshToken
) {
}
