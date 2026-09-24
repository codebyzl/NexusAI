package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountStatus;
import io.github.codebyzl.nexus.auth.account.domain.Email;

final class AccountPersistenceConverter {

    private AccountPersistenceConverter() {
    }

     static AccountPo toPo(Account account) {
        // Account → AccountPo
        AccountPo po = new AccountPo();

        po.setId(account.getId());
        po.setEmail(account.getEmail().value());
        po.setPasswordHash(account.getPasswordHash());
        po.setStatus(account.getStatus().getCode());
        po.setEmailVerifiedAt(account.getEmailVerifiedAt());
        po.setCreatedAt(account.getCreatedAt());
        po.setUpdatedAt(account.getUpdatedAt());

        return po;
    }


     static Account toDomain(AccountPo po) {
        // AccountPo → Account.restore(...)
         return Account.restore(
                 po.getId(),
                 new Email(po.getEmail()) ,
                 po.getPasswordHash(),
                 AccountStatus.fromCode(po.getStatus()),
                 po.getEmailVerifiedAt(),
                 po.getCreatedAt(),
                 po.getUpdatedAt()
         );

    }
}