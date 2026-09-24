package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Test
    void insertsAndFindsAccount() {
        AccountPo account = new AccountPo();
        account.setEmail("mapper-test@example.com");
        account.setPasswordHash("test-password-hash");
        account.setStatus(1);

        int affectedRows = accountMapper.insert(account);

        assertThat(affectedRows).isEqualTo(1);
        assertThat(account.getId()).isNotNull();

        AccountPo saved = accountMapper.selectById(account.getId());

        assertThat(saved).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("mapper-test@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("test-password-hash");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}