package io.github.codebyzl.nexus.auth.account.application;

import io.github.codebyzl.nexus.auth.account.application.exception.EmailAlreadyRegisteredException;
import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import io.github.codebyzl.nexus.auth.application.security.PasswordHasher;
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
public class RegisterAccountService {
    private final AccountRepository accountRepository;
    private final PasswordHasher passwordHasher;

    @Transactional
    public RegisterAccountResult register(RegisterAccountCommand command) {
        // 1. 转换为领域值对象（由 Email 保证邮箱约束）
        Email email = new Email(command.email());

        // 2. 检查邮箱是否已经注册
        if (accountRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        // 3. 密码哈希
        String passwordHash = passwordHasher.hash(command.rawPassword());

        // 4. 创建领域实体
        Account account = Account.register(email, passwordHash);

        // 5. 持久化

        Account savedAccount = accountRepository.save(account);

        return new RegisterAccountResult(
                savedAccount.getId(),
                savedAccount.getEmail().value()
        );
    }
}