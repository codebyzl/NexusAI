package io.github.codebyzl.nexus.auth.infrastructure.security.token;

import io.github.codebyzl.nexus.auth.application.security.token.IssuedRefreshToken;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenHasher;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Component
@RequiredArgsConstructor
public class SecureRandomRefreshTokenIssuer implements RefreshTokenIssuer {

    private final RefreshTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenHasher refreshTokenHasher;

    @Override
    public IssuedRefreshToken issue() {
        return issue(
                Instant.now().plus(properties.ttl())
        );
    }

    @Override
    public IssuedRefreshToken issue(Instant expiresAt) {
        Instant now = Instant.now();

        if (expiresAt == null || !expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Refresh Token expiration must be in the future"
            );
        }

        // 1. 生成配置指定长度的随机数据
        byte[] bytes = new byte[properties.byteLength()];
        secureRandom.nextBytes(bytes);

        // 2. 编码成 Refresh Token 原文
        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        // 3. 计算 SHA-256 哈希
        String hash = refreshTokenHasher.hash(value);

        return new IssuedRefreshToken(
                value,
                hash,
                expiresAt
        );
    }
}