package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import java.time.Instant;

public record RotateRefreshTokenResponse(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
