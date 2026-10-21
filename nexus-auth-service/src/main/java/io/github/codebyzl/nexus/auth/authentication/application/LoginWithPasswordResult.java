package io.github.codebyzl.nexus.auth.authentication.application;

import java.time.Instant;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record LoginWithPasswordResult(
        Long accountId,
        String email,
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt
) {
}
