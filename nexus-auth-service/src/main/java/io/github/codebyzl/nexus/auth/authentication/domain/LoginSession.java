package io.github.codebyzl.nexus.auth.authentication.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
public class LoginSession {

    private final UUID id;
    private final Long accountId;

    private String refreshTokenHash;
    private LoginSessionStatus status;

    private final Instant createdAt;
    private Instant updatedAt;
    private final Instant expiresAt;
    private Instant revokedAt;

    private LoginSession(
            UUID id,
            Long accountId,
            String refreshTokenHash,
            LoginSessionStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.refreshTokenHash = Objects.requireNonNull(refreshTokenHash);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.revokedAt = revokedAt;
    }

    // 创建新登录会话
    public static LoginSession create(
            Long accountId,
            String refreshTokenHash,
            Instant now,
            Instant expiresAt
    ) {
        Objects.requireNonNull(now);
        Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Expiration must be after creation time"
            );
        }

        if (refreshTokenHash == null || refreshTokenHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh token hash must not be blank"
            );
        }

        return new LoginSession(
                UUID.randomUUID(),
                accountId,
                refreshTokenHash,
                LoginSessionStatus.ACTIVE,
                now,
                now,
                expiresAt,
                null
        );
    }

    // 从数据库恢复领域实体
    public static LoginSession restore(
            UUID id,
            Long accountId,
            String refreshTokenHash,
            LoginSessionStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt,
            Instant revokedAt
    ) {
        return new LoginSession(
                id,
                accountId,
                refreshTokenHash,
                status,
                createdAt,
                updatedAt,
                expiresAt,
                revokedAt
        );
    }

    // 判断会话是否可用
    public boolean isActiveAt(Instant now) {
        return status == LoginSessionStatus.ACTIVE
                && now.isBefore(expiresAt);
    }

    // 撤销会话（AUTH-05 使用）
    public void revoke(Instant now) {
        if (status == LoginSessionStatus.REVOKED) {
            return;
        }

        status = LoginSessionStatus.REVOKED;
        revokedAt = now;
        updatedAt = now;
    }
}