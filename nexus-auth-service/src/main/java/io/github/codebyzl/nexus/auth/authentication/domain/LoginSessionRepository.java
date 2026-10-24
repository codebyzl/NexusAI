package io.github.codebyzl.nexus.auth.authentication.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepository {

    LoginSession save(LoginSession session);

    Optional<LoginSession> findById(
            UUID sessionId
    );

    Optional<LoginSession> findByIdForUpdate(
            UUID sessionId
    );

    void update(LoginSession session);

    List<LoginSession> findByAccountId(Long accountId);

    List<LoginSession> findOtherActiveForUpdate(
            Long accountId,
            UUID currentSessionId
    );

    List<LoginSession> findByAccountIdForUpdate(Long accountId);
}
