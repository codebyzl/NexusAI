package io.github.codebyzl.nexus.auth.authentication.application.session;


/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record RevokeLoginSessionCommand(
        Long accountId,
        String sessionId
) {
}