package io.github.codebyzl.nexus.auth.authentication.application;

public record LogoutCommand(
        String refreshToken
) {
}
