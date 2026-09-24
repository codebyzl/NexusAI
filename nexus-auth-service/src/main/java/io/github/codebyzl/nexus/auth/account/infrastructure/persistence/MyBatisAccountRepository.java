package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.codebyzl.nexus.auth.account.domain.Account;
import io.github.codebyzl.nexus.auth.account.domain.AccountRepository;
import io.github.codebyzl.nexus.auth.account.domain.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MyBatisAccountRepository implements AccountRepository {

    private final AccountMapper accountMapper;

    @Override
    public Account save(Account account) {

        Long id = account.getId();

        // Account → AccountPo
        AccountPo accountPo = AccountPersistenceConverter.toPo(account);

        if (id == null) {
            // 新账号：插入
            // Mapper insert
            int inserted = accountMapper.insert(accountPo);
            if (inserted != 1) {
                throw new IllegalStateException("账号插入失败");
            }
            id = accountPo.getId();
        } else {
            // 已有账号：只更新当前领域模型允许变化的字段
            int rows = accountMapper.update(
                    null,
                    new LambdaUpdateWrapper<AccountPo>()
                            .eq(AccountPo::getId, id)
                            .set(AccountPo::getStatus, accountPo.getStatus())
                            .set(AccountPo::getEmailVerifiedAt, accountPo.getEmailVerifiedAt())
            );
            if (rows != 1) {
                throw new IllegalStateException("账号更新失败或账号不存在");
            }
        }

        // 根据回填 ID 重新查询
        AccountPo savedPo = accountMapper.selectById(id);
        if (savedPo == null) {
            throw new IllegalStateException("保存后未找到账号");
        }
        // AccountPo → Account
        return AccountPersistenceConverter.toDomain(savedPo);
    }

    @Override
    public Optional<Account> findById(Long accountId) {
        if (accountId == null) {
            throw new IllegalStateException("查询参数异常");
        }
        AccountPo accountPo = accountMapper.selectById(accountId);

        return Optional.ofNullable(accountPo).map(AccountPersistenceConverter::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(Email email) {

        AccountPo accountPo = accountMapper.selectOne(
                new LambdaQueryWrapper<AccountPo>()
                        .eq(AccountPo::getEmail, email.value())
        );

        return Optional.ofNullable(accountPo).map(AccountPersistenceConverter::toDomain);
    }

    /**
     * 判断是否存在这个Email的账户
     * @param email
     * @return True/False
     */
    @Override
    public boolean existsByEmail(Email email) {
        return accountMapper.exists(
                new LambdaQueryWrapper<AccountPo>()
                        .eq(AccountPo::getEmail, email.value())
        );
    }
}