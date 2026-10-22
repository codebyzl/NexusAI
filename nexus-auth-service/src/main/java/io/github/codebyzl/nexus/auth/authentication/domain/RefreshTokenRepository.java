package io.github.codebyzl.nexus.auth.authentication.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void insert(RefreshToken refreshToken);

    void update(RefreshToken refreshToken);

    Optional<RefreshToken> findByHash(String tokenHash);

    Optional<RefreshToken> findByHashForUpdate(String tokenHash);

    int revokeActiveBySessionId(UUID sessionId, Instant revokedAt);
}
