package io.github.codebyzl.nexus.auth.authentication.application.session;

import java.util.UUID;

public record ListLoginSessionsQuery(
        Long accountId,
        UUID currentSessionId
) {
}