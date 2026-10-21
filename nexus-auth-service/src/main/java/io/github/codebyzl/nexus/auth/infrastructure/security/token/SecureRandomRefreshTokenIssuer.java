package io.github.codebyzl.nexus.auth.infrastructure.security.token;

import io.github.codebyzl.nexus.auth.application.security.token.IssuedRefreshToken;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

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

    @Override
    public IssuedRefreshToken issue() {
        // 1. 生成配置指定长度的随机数据
        byte[] bytes = new byte[properties.byteLength()];
        secureRandom.nextBytes(bytes);

        // 2. 编码成 Refresh Token 原文
        String value = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        // 3. 计算 SHA-256 哈希
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    exception
            );
        }
        byte[] hashBytes = digest.digest(
                value.getBytes(StandardCharsets.UTF_8)
        );

        // 4. 转换成十六进制字符串
        String hash = HexFormat.of().formatHex(hashBytes);
        Instant expiresAt = Instant.now().plus(properties.ttl());

        return new IssuedRefreshToken(
                value,
                hash,
                expiresAt
        );
    }
}