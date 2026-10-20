package io.github.codebyzl.nexus.auth.application.security;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:签发token
 */
public interface AccessTokenIssuer {
    IssuedAccessToken issue(Long accountId);
}
