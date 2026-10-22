package io.github.codebyzl.nexus.auth.authentication.application;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.application.security.jwt.AccessTokenIssuer;
import io.github.codebyzl.nexus.auth.application.security.jwt.IssuedAccessToken;
import io.github.codebyzl.nexus.auth.application.security.token.IssuedRefreshToken;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenHasher;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenIssuer;
import io.github.codebyzl.nexus.auth.authentication.application.exception.InvalidRefreshTokenException;
import io.github.codebyzl.nexus.auth.authentication.application.exception.RefreshTokenReuseDetectedException;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshToken;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RotateRefreshTokenService {

    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final AccountRepository accountRepository;

    @Transactional(
            noRollbackFor =
                    RefreshTokenReuseDetectedException.class
    )
    public RotateRefreshTokenResult rotate(
            RotateRefreshTokenCommand command
    ) {
        if (command == null
                || command.refreshToken() == null
                || command.refreshToken().isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = refreshTokenHasher.hash(
                command.refreshToken()
        );

        /*
         * 第一次普通查询只用于获取 sessionId。
         * 不能基于这次查询结果做最终状态判断。
         */
        RefreshToken tokenSnapshot =
                refreshTokenRepository.findByHash(tokenHash)
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        /*
         * 统一先锁 Session，保证同一个 Token Family
         * 的刷新、重用检测和撤销串行执行。
         */
        LoginSession session =
                loginSessionRepository.findByIdForUpdate(
                                tokenSnapshot.getSessionId()
                        )
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        /*
         * Session 锁定后重新读取并锁定 Token。
         * 这里读到的状态才用于最终业务判断。
         */
        RefreshToken currentToken =
                refreshTokenRepository.findByHashForUpdate(
                                tokenHash
                        )
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        if (!currentToken.getSessionId()
                .equals(session.getId())) {
            throw new InvalidRefreshTokenException();
        }

        Instant now = Instant.now();

        /*
         * USED 表示这个 Token 已经完成过一次轮换。
         * 再次出现时撤销整个 Session。
         */
        if (currentToken.getStatus()
                == RefreshTokenStatus.USED) {
            revokeTokenFamily(
                    session,
                    now
            );

            throw new RefreshTokenReuseDetectedException();
        }

        if (!session.isActiveAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        if (!currentToken.isActiveAt(now)) {
            throw new InvalidRefreshTokenException();
        }

        Account account =
                accountRepository.findById(
                                session.getAccountId()
                        )
                        .filter(Account::canLogin)
                        .orElseThrow(
                                InvalidRefreshTokenException::new
                        );

        /*
         * 后继 Refresh Token 继承 Session 的绝对过期时间，
         * 不重新从当前时间增加七天。
         */
        IssuedRefreshToken issuedRefreshToken =
                refreshTokenIssuer.issue(
                        session.getExpiresAt()
                );

        RefreshToken successorToken =
                RefreshToken.create(
                        session.getId(),
                        issuedRefreshToken.hash(),
                        now,
                        issuedRefreshToken.expiresAt()
                );

        /*
         * 必须先插入后继 Token。
         * replaced_by_token_id 有外键约束，
         * 旧 Token 更新前后继记录必须已经存在。
         */
        refreshTokenRepository.insert(
                successorToken
        );

        currentToken.markUsed(
                successorToken.getId(),
                now
        );

        refreshTokenRepository.update(
                currentToken
        );

        IssuedAccessToken issuedAccessToken =
                accessTokenIssuer.issue(
                        account.getId(),
                        session.getId()
                );

        return new RotateRefreshTokenResult(
                issuedAccessToken.value(),
                issuedAccessToken.expiresAt(),
                issuedRefreshToken.value(),
                issuedRefreshToken.expiresAt()
        );
    }

    private void revokeTokenFamily(
            LoginSession session,
            Instant now
    ) {
        session.revoke(now);

        loginSessionRepository.update(session);

        refreshTokenRepository.revokeActiveBySessionId(
                session.getId(),
                now
        );
    }
}
