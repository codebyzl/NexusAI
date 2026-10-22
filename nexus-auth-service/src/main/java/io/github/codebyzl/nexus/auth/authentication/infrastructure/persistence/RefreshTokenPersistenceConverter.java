package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.authentication.domain.RefreshToken;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenStatus;

import java.util.UUID;

final class RefreshTokenPersistenceConverter {

    private RefreshTokenPersistenceConverter() {
    }

    static RefreshTokenPo toPo(RefreshToken refreshToken) {
        RefreshTokenPo po = new RefreshTokenPo();

        po.setId(refreshToken.getId().toString());
        po.setSessionId(refreshToken.getSessionId().toString());
        po.setTokenHash(refreshToken.getTokenHash());
        po.setStatus(refreshToken.getStatus().getCode());
        po.setExpiresAt(refreshToken.getExpiresAt());
        po.setCreatedAt(refreshToken.getCreatedAt());
        po.setUsedAt(refreshToken.getUsedAt());
        po.setRevokedAt(refreshToken.getRevokedAt());
        po.setReplacedByTokenId(
                refreshToken.getReplacedByTokenId() == null
                        ? null
                        : refreshToken.getReplacedByTokenId().toString()
        );

        return po;
    }

    static RefreshToken toDomain(RefreshTokenPo po) {
        return RefreshToken.restore(
                UUID.fromString(po.getId()),
                UUID.fromString(po.getSessionId()),
                po.getTokenHash(),
                RefreshTokenStatus.fromCode(po.getStatus()),
                po.getCreatedAt(),
                po.getExpiresAt(),
                po.getUsedAt(),
                po.getRevokedAt(),
                po.getReplacedByTokenId() == null
                        ? null
                        : UUID.fromString(po.getReplacedByTokenId())
        );
    }
}
