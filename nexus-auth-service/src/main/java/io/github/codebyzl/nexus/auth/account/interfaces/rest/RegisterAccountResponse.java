package io.github.codebyzl.nexus.auth.account.interfaces.rest;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record RegisterAccountResponse(
        Long accountId,
        String email
) {
}