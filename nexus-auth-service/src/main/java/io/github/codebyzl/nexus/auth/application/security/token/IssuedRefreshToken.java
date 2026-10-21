package io.github.codebyzl.nexus.auth.application.security.token;

import java.time.Instant;

public record IssuedRefreshToken(
        String value,
        String hash,
        Instant expiresAt
) {
}
