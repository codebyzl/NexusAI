package io.github.codebyzl.nexus.auth.account.domain;

import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Getter
public class Account {

    private final Long id;
    private final Email email;
    private final String passwordHash;
    private AccountStatus status;

    private LocalDateTime emailVerifiedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Account(
            Long id,
            Email email,
            String passwordHash,
            AccountStatus status,
            LocalDateTime emailVerifiedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        // 新建与恢复都必须满足的规则
        if (email == null) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (!StringUtils.hasText(passwordHash)) {
            throw new IllegalArgumentException("密码哈希不能为空");
        }
        if (status == null) {
            throw new IllegalArgumentException("账号状态不能为空");
        }
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.emailVerifiedAt = emailVerifiedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Account register(Email email, String passwordHash) {
        return new Account(
                null,
                email,
                passwordHash,
                AccountStatus.ACTIVE,
                null,
                null,
                null
        );
    }
    public static Account restore(
            Long id,
            Email email,
            String passwordHash,
            AccountStatus status,
            LocalDateTime emailVerifiedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        // 已入库的账号应当有主键和创建、更新时间
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("已保存账号的 ID 无效");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("已保存账号缺少创建或更新时间");
        }
        return new Account(
                id,
                email,
                passwordHash,
                status,
                emailVerifiedAt,
                createdAt,
                updatedAt
        );
    }

    public boolean canLogin() {
        return status == AccountStatus.ACTIVE;
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    public void lock() {
        this.status = AccountStatus.LOCKED;
    }

    public void disable() {
        this.status = AccountStatus.DISABLED;
    }
}