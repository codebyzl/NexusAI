package io.github.codebyzl.nexus.auth.authentication.domain;

public interface LoginSessionRepository {

    LoginSession save(LoginSession session);
}
