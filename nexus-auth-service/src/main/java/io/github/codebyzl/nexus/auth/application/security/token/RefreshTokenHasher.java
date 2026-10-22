package io.github.codebyzl.nexus.auth.application.security.token;

public interface RefreshTokenHasher {

    String hash(String rawRefreshToken);
}
