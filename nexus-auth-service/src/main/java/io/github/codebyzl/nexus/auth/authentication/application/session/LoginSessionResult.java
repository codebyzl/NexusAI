package io.github.codebyzl.nexus.auth.authentication.application.session;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record LoginSessionResult(
        UUID sessionId,
        LoginSessionStatus status,
        boolean current,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt
) {
}
