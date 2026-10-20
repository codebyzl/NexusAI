package io.github.codebyzl.nexus.auth.authentication.application;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import io.github.codebyzl.nexus.auth.application.security.AccessTokenIssuer;
import io.github.codebyzl.nexus.auth.application.security.IssuedAccessToken;
import io.github.codebyzl.nexus.auth.application.security.PasswordHasher;
import io.github.codebyzl.nexus.auth.authentication.application.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public LoginWithPasswordResult login(LoginWithPasswordCommand command) {
        Email email = new Email(command.email());

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!account.canLogin()
                || !passwordHasher.matches(
                        command.rawPassword(),
                        account.getPasswordHash()
                )) {
            throw new InvalidCredentialsException();
        }

        IssuedAccessToken accessToken = accessTokenIssuer.issue(account.getId());

        return new LoginWithPasswordResult(
                account.getId(),
                account.getEmail().value(),
                accessToken.value(),
                accessToken.expiresAt()
        );
    }
}