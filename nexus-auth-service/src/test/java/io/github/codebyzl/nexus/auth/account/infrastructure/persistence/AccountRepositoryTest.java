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
public class AccountRepositoryTest {
    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void accountSaveTest() {
        Account account = Account.register(new Email("1324533233@qq.com"), "dwdqwdqwdq");
        Account saved = accountRepository.save(account);

        assertThat(saved)
                .usingRecursiveComparison()
                .ignoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(account);
    }

    @Test
    public void accountFindByIdTest() {
        Account account = accountRepository.findById(1l).orElse(null);
        assertThat(account).isNotNull();
    }

    @Test
    public void accountFindByEmailTest() {
        Account account = accountRepository.findByEmail(new Email("1324533233@qq.com")).orElse(null);
        assertThat(account).isNotNull();
    }
}