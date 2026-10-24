package io.github.codebyzl.nexus.auth.authentication.application.session;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Service
@RequiredArgsConstructor
public class RevokeAllSessionsService {
    private final LoginSessionRepository loginSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void revokeAll(RevokeAllSessionsQuery query) {
        List<LoginSession> sessions = loginSessionRepository
                .findByAccountIdForUpdate(query.accountId());
        Instant now = Instant.now();

        for (LoginSession session : sessions) {

            // 1. 撤销登录会话
            session.revoke(now);

            // 2. 更新数据库 Session 状态
            loginSessionRepository.update(session);

            // 3. 撤销这个 Session 下的活动 Refresh Token
            refreshTokenRepository.revokeActiveBySessionId(
                    session.getId(),
                    now
            );
        }
    }
}
