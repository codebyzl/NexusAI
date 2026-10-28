package io.github.codebyzl.nexus.auth.authentication.application;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import io.github.codebyzl.nexus.auth.application.security.jwt.AccessTokenIssuer;
import io.github.codebyzl.nexus.auth.application.security.jwt.IssuedAccessToken;
import io.github.codebyzl.nexus.auth.application.security.password.PasswordHasher;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptContext;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptDecision;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptLimiter;
import io.github.codebyzl.nexus.auth.application.security.token.IssuedRefreshToken;
import io.github.codebyzl.nexus.auth.application.security.token.RefreshTokenIssuer;
import io.github.codebyzl.nexus.auth.authentication.application.exception.InvalidCredentialsException;
import io.github.codebyzl.nexus.auth.authentication.application.exception.LoginRateLimitedException;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshToken;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Service
@RequiredArgsConstructor
public class LoginWithPasswordService {

    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptLimiter loginAttemptLimiter;

    @Transactional
    public LoginWithPasswordResult login(LoginWithPasswordCommand command) {
        // 核对账号密码和账号状态
        Email email = new Email(command.email());


        LoginAttemptContext context = new LoginAttemptContext(
                email.value(),
                command.clientIp()
        );

        LoginAttemptDecision decision = loginAttemptLimiter.check(context);

        if (!decision.allowed()) {
            throw new LoginRateLimitedException(decision.retryAfter());
        }

        Account account = accountRepository.findByEmail(email)
                .orElse(null);

        if (account == null
                || !account.canLogin()
                || !passwordHasher.matches(
                command.rawPassword(),
                account.getPasswordHash()
        )) {
            LoginAttemptDecision failure =
                    loginAttemptLimiter.recordFailure(context);

            if (!failure.allowed()) {
                throw new LoginRateLimitedException(failure.retryAfter());
            }

            throw new InvalidCredentialsException();
        }

        IssuedRefreshToken issuedRefreshToken = refreshTokenIssuer.issue();
        Instant now = Instant.now();

        LoginSession session = LoginSession.create(
                account.getId(),
                now,
                issuedRefreshToken.expiresAt()
        );

        LoginSession savedSession = loginSessionRepository.save(session);

        RefreshToken refreshTokenRecord =
                RefreshToken.create(
                        savedSession.getId(),
                        issuedRefreshToken.hash(),
                        now,
                        issuedRefreshToken.expiresAt()
                );

        refreshTokenRepository.insert(refreshTokenRecord);


        loginAttemptLimiter.recordSuccess(context);

        IssuedAccessToken accessToken = accessTokenIssuer.issue(
                account.getId(),
                savedSession.getId()
        );

        return new LoginWithPasswordResult(
                account.getId(),
                account.getEmail().value(),
                accessToken.value(),
                accessToken.expiresAt(),
                issuedRefreshToken.value(),
                issuedRefreshToken.expiresAt()
        );
    }
}
