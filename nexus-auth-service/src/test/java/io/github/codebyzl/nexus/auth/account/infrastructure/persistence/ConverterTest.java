package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountStatus;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public class ConverterTest {
    @Test
    public void toDomain() {
        // Arrange：准备数据库对象
        LocalDateTime emailVerifiedAt =
                LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 18, 9, 30);

        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 9, 21, 15, 20);

        AccountPo po = new AccountPo();
        po.setId(100L);
        po.setEmail("user@example.com");
        po.setPasswordHash("hashed-password");
        po.setStatus(2);
        po.setEmailVerifiedAt(emailVerifiedAt);
        po.setCreatedAt(createdAt);
        po.setUpdatedAt(updatedAt);

        Account account = AccountPersistenceConverter.toDomain(po);

        // Assert：检查转换结果
        assertThat(account.getId()).isEqualTo(100L);
        assertThat(account.getEmail())
                .isEqualTo(new Email("user@example.com"));
        assertThat(account.getPasswordHash())
                .isEqualTo("hashed-password");
        assertThat(account.getStatus())
                .isEqualTo(AccountStatus.LOCKED);
        assertThat(account.getEmailVerifiedAt())
                .isEqualTo(emailVerifiedAt);
        assertThat(account.getCreatedAt())
                .isEqualTo(createdAt);
        assertThat(account.getUpdatedAt())
                .isEqualTo(updatedAt);
    }

    @Test
    public void fromDomain() {
        // Arrange：准备领域实体
        LocalDateTime emailVerifiedAt =
                LocalDateTime.of(2026, 9, 20, 10, 0);

        LocalDateTime createdAt =
                LocalDateTime.of(2026, 9, 18, 9, 30);

        LocalDateTime updatedAt =
                LocalDateTime.of(2026, 9, 21, 15, 20);

        Account account = Account.restore(
                100L,
                new Email("user@example.com"),
                "hashed-password",
                AccountStatus.LOCKED,
                emailVerifiedAt,
                createdAt,
                updatedAt
        );

        // Act：执行 Domain → PO
        AccountPo po =
                AccountPersistenceConverter.toPo(account);

        // Assert：检查转换结果
        assertThat(po.getId()).isEqualTo(100L);
        assertThat(po.getEmail())
                .isEqualTo("user@example.com");
        assertThat(po.getPasswordHash())
                .isEqualTo("hashed-password");
        assertThat(po.getStatus()).isEqualTo(2);
        assertThat(po.getEmailVerifiedAt())
                .isEqualTo(emailVerifiedAt);
        assertThat(po.getCreatedAt())
                .isEqualTo(createdAt);
        assertThat(po.getUpdatedAt())
                .isEqualTo(updatedAt);
    }
}