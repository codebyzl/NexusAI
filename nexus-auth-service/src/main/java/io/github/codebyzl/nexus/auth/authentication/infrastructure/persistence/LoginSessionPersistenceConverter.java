package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionStatus;


import java.util.UUID;

final class LoginSessionPersistenceConverter {

    private LoginSessionPersistenceConverter() {
    }

    // LoginSession → LoginSessionPo
    static LoginSessionPo toPo(LoginSession session) {
        LoginSessionPo po = new LoginSessionPo();

        po.setId(session.getId().toString());
        po.setAccountId(session.getAccountId());
        po.setRefreshTokenHash(session.getRefreshTokenHash());
        po.setStatus(session.getStatus().getCode());
        po.setCreatedAt(session.getCreatedAt());
        po.setUpdatedAt(session.getUpdatedAt());
        po.setExpiresAt(session.getExpiresAt());
        po.setRevokedAt(session.getRevokedAt());

        return po;
    }

    // LoginSessionPo → LoginSession
    static LoginSession toDomain(LoginSessionPo po) {
        return LoginSession.restore(
                UUID.fromString(po.getId()),
                po.getAccountId(),
                po.getRefreshTokenHash(),
                LoginSessionStatus.fromCode(po.getStatus()),
                po.getCreatedAt(),
                po.getUpdatedAt(),
                po.getExpiresAt(),
                po.getRevokedAt()
        );
    }
}