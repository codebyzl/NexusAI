package io.github.codebyzl.nexus.auth.account.domain;

import lombok.Getter;

@Getter
public class Account {

    private final Long id;
    private final Email email;
    private final String passwordHash;
    private AccountStatus status;

    private Account(
            Long id,
            Email email,
            String passwordHash,
            AccountStatus status
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public boolean canLogin() {
        return status == AccountStatus.ACTIVE;
    }

    public void allow() {
        this.status = AccountStatus.ACTIVE;
    }

    public void lock() {
        this.status = AccountStatus.LOCKED;
    }

    public void disable() {
        this.status = AccountStatus.DISABLED;
    }
}