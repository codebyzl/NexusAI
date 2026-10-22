package io.github.codebyzl.nexus.auth.authentication.application;

import java.time.Instant;

public record RotateRefreshTokenResult(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
