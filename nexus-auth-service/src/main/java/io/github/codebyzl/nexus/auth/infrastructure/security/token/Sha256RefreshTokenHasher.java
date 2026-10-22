package io.github.codebyzl.nexus.auth.infrastructure.security.token;

import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenHasher;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class Sha256RefreshTokenHasher implements RefreshTokenHasher {

    @Override
    public String hash(String rawRefreshToken) {
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh Token must not be blank"
            );
        }

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
                rawRefreshToken.getBytes(
                        StandardCharsets.UTF_8
                )
        );

        return HexFormat.of().formatHex(hashBytes);
    }
}
