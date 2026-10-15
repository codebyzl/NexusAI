package io.github.codebyzl.nexus.auth.account.application;

import io.github.codebyzl.nexus.auth.account.application.PasswordHasher;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountCommand;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountService;
import io.github.codebyzl.nexus.auth.account.application.exception.EmailAlreadyRegisteredException;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class RegisterAccountServiceTest {

    @Autowired
    private RegisterAccountService service;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Test
    void shouldRegisterSuccessfully() {
        String rawPassword = "12345678";

        var command = new RegisterAccountCommand(
                "test-register@example.com",
                rawPassword
        );

        // 真正执行注册
        var result = service.register(command);

        // 验证返回 ID
        assertNotNull(result.accountId());

        // 从真实数据库读取账号
        var account = accountRepository
                .findById(result.accountId())
                .orElseThrow();

        // 验证保存的不是明文密码
        assertNotEquals(rawPassword, account.getPasswordHash());

    }

    @Test
    void shouldRejectDuplicateEmail() {
        String email = "test-duplicate@example.com";

        var command = new RegisterAccountCommand(
                email,
                "12345678"
        );

        // 第一次注册成功
        service.register(command);

        // 第二次注册相同邮箱
        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> service.register(command)
        );
    }
}