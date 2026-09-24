package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import io.github.codebyzl.nexus.auth.support.TestcontainersConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    private Account saveAccount(String email) {
        Account account = Account.register(
                new Email(email),
                "dwdqwdqwdq"
        );

        return accountRepository.save(account);
    }

    @Test
    void accountSaveTest() {
        Account account = Account.register(
                new Email("save@qq.com"),
                "dwdqwdqwdq"
        );

        Account saved = accountRepository.save(account);

        assertThat(saved)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(account);
    }

    @Test
    void accountFindByIdTest() {
        Account saved = saveAccount("find-id@qq.com");

        Account found = accountRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(found)
                .usingRecursiveComparison()
                .isEqualTo(saved);
    }

    @Test
    void accountFindByEmailTest() {
        Account saved = saveAccount("find-email@qq.com");

        Account found = accountRepository
                .findByEmail(saved.getEmail())
                .orElseThrow();

        assertThat(found)
                .usingRecursiveComparison()
                .isEqualTo(saved);
    }
}