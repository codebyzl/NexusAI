package io.github.codebyzl.nexus.auth.application.security.token;

public interface RefreshTokenIssuer {

    IssuedRefreshToken issue();
}
