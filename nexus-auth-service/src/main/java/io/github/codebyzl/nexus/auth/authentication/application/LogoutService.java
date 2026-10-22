package io.github.codebyzl.nexus.auth.authentication.application;

import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenHasher;
import io.github.codebyzl.nexus.auth.authentication.application.exception.InvalidRefreshTokenException;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionStatus;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshToken;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginSessionRepository loginSessionRepository;

    @Transactional
    public void logout(LogoutCommand command) {
        if (command == null
                || command.refreshToken() == null
                || command.refreshToken().isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = refreshTokenHasher.hash(
                command.refreshToken()
        );

        /*
         * 第一次查询只用于得到 sessionId。
         * 为了让退出具备幂等性，未知 Token 直接返回。
         */
        Optional<RefreshToken> tokenSnapshotOptional =
                refreshTokenRepository.findByHash(
                        tokenHash
                );

        if (tokenSnapshotOptional.isEmpty()) {
            return;
        }

        RefreshToken tokenSnapshot =
                tokenSnapshotOptional.get();

        /*
         * 先锁定整个 Session，和刷新服务保持一致的锁顺序。
         */
        Optional<LoginSession> sessionOptional =
                loginSessionRepository.findByIdForUpdate(
                        tokenSnapshot.getSessionId()
                );

        if (sessionOptional.isEmpty()) {
            return;
        }

        LoginSession session = sessionOptional.get();

        Optional<RefreshToken> lockedTokenOptional =
                refreshTokenRepository.findByHashForUpdate(
                        tokenHash
                );

        if (lockedTokenOptional.isEmpty()) {
            return;
        }

        RefreshToken lockedToken =
                lockedTokenOptional.get();

        if (!lockedToken.getSessionId()
                .equals(session.getId())) {
            return;
        }

        Instant now = Instant.now();

        /*
         * LoginSession.revoke() 本身是幂等的，
         * 这里避免对已经撤销的 Session 执行无意义 UPDATE。
         */
        if (session.getStatus()
                != LoginSessionStatus.REVOKED) {
            session.revoke(now);
            loginSessionRepository.update(session);
        }

        /*
         * 只撤销 ACTIVE Token。
         *
         * USED Token 继续保持 USED，保留完整轮换和重放证据。
         * REVOKED Token 不需要重复更新。
         */
        refreshTokenRepository.revokeActiveBySessionId(
                session.getId(),
                now
        );
    }
}
