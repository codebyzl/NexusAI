package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Repository
@RequiredArgsConstructor
public class MyBatisLoginSessionRepository implements LoginSessionRepository {

    private final LoginSessionMapper loginSessionMapper;

    @Override
    public LoginSession save(LoginSession session) {
        LoginSessionPo po = LoginSessionPersistenceConverter.toPo(session);

        int inserted = loginSessionMapper.insert(po);
        if (inserted != 1) {
            throw new IllegalStateException("登录会话创建失败");
        }

        return session;
    }
}
