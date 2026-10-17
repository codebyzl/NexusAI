package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record LoginWithPasswordResponse(
        Long accountId,
        String email
) {
}