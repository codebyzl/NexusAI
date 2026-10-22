package io.github.codebyzl.nexus.auth.authentication.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
public class RefreshToken {

    private static final Pattern SHA_256_HEX_PATTERN =
            Pattern.compile("[0-9a-f]{64}");

    private final UUID id;
    private final UUID sessionId;
    private final String tokenHash;
    private RefreshTokenStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;
    private UUID replacedByTokenId;

    private RefreshToken(
            UUID id,
            UUID sessionId,
            String tokenHash,
            RefreshTokenStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt,
            UUID replacedByTokenId
    ) {
        this.id = Objects.requireNonNull(id, "Refresh token ID must not be null");
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID must not be null");
        this.tokenHash = requireValidTokenHash(tokenHash);
        this.status = Objects.requireNonNull(status, "Refresh token status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation time must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "Expiration time must not be null");
        this.usedAt = usedAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "Expiration time must be after creation time"
            );
        }
    }

    public static RefreshToken create(
            UUID sessionId,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        return new RefreshToken(
                UUID.randomUUID(),
                sessionId,
                tokenHash,
                RefreshTokenStatus.ACTIVE,
                createdAt,
                expiresAt,
                null,
                null,
                null
        );
    }

    public static RefreshToken restore(
            UUID id,
            UUID sessionId,
            String tokenHash,
            RefreshTokenStatus status,
            Instant createdAt,
            Instant expiresAt,
            Instant usedAt,
            Instant revokedAt,
            UUID replacedByTokenId
    ) {
        return new RefreshToken(
                id,
                sessionId,
                tokenHash,
                status,
                createdAt,
                expiresAt,
                usedAt,
                revokedAt,
                replacedByTokenId
        );
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return !now.isBefore(expiresAt);
    }

    public boolean isActiveAt(Instant now) {
        return status == RefreshTokenStatus.ACTIVE
                && !isExpiredAt(now);
    }

    public void markUsed(UUID successorTokenId, Instant now) {
        Objects.requireNonNull(successorTokenId, "Successor token ID must not be null");
        Objects.requireNonNull(now, "Usage time must not be null");

        if (status != RefreshTokenStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only an active refresh token can be marked as used"
            );
        }

        if (isExpiredAt(now)) {
            throw new IllegalStateException(
                    "An expired refresh token cannot be marked as used"
            );
        }

        status = RefreshTokenStatus.USED;
        usedAt = now;
        replacedByTokenId = successorTokenId;
    }

    public void revoke(Instant now) {
        Objects.requireNonNull(now, "Revocation time must not be null");

        if (status == RefreshTokenStatus.REVOKED) {
            return;
        }

        if (status == RefreshTokenStatus.USED) {
            throw new IllegalStateException(
                    "A used refresh token cannot be revoked"
            );
        }

        status = RefreshTokenStatus.REVOKED;
        revokedAt = now;
    }

    private static String requireValidTokenHash(String tokenHash) {
        if (tokenHash == null || !SHA_256_HEX_PATTERN.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException(
                    "Refresh token SHA-256 hash must contain 64 lowercase hexadecimal characters"
            );
        }

        return tokenHash;
    }
}
