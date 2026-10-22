package io.github.codebyzl.nexus.auth.application.security.token;

import java.time.Instant;

public interface RefreshTokenIssuer {

    IssuedRefreshToken issue();

    IssuedRefreshToken issue(Instant expiresAt);
}
