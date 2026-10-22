package io.github.codebyzl.nexus.auth.authentication.domain;

import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepository {

    LoginSession save(LoginSession session);

    Optional<LoginSession> findByIdForUpdate(
            UUID sessionId
    );

    void update(LoginSession session);
}
