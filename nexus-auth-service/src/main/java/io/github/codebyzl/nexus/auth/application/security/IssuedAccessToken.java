package io.github.codebyzl.nexus.auth.application.security;

import java.time.Instant;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record IssuedAccessToken(
        String value,
        Instant expiresAt
) {
}
