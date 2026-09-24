package io.github.codebyzl.nexus.auth.account.domain;

import java.util.Optional;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(Long accountId);

    Optional<Account> findByEmail(Email email);

    boolean existsByEmail(Email email);
}