package io.github.codebyzl.nexus.auth.authentication.application.session;

import java.util.UUID;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record RevokeOtherSessionsQuery(
        Long accountId,
        UUID currentSessionId
) {
}