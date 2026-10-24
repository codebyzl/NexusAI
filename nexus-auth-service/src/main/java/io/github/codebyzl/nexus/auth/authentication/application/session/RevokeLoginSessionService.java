package io.github.codebyzl.nexus.auth.authentication.application.session;

import io.github.codebyzl.nexus.auth.authentication.application.exception.SessionNotFoundException;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionStatus;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RevokeLoginSessionService {

    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void revoke(RevokeLoginSessionCommand command) {

        UUID sessionId;
        try {
            sessionId = UUID.fromString(command.sessionId());
        } catch (IllegalArgumentException e) {
            throw new SessionNotFoundException();
        }

        LoginSession session = loginSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(SessionNotFoundException::new);

        // 确保只能撤销自己的 Session
        if (!session.getAccountId().equals(command.accountId())) {
            throw new SessionNotFoundException();
        }

        Instant now = Instant.now();

        // 撤销 LoginSession
        if (session.getStatus() != LoginSessionStatus.REVOKED) {
            session.revoke(now);
            loginSessionRepository.update(session);
        }

        // 撤销该 Session 下所有 ACTIVE 的 Refresh Token
        refreshTokenRepository.revokeActiveBySessionId(
                session.getId(),
                now
        );
    }
}