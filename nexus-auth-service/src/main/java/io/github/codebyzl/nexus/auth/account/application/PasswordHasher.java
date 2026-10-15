package io.github.codebyzl.nexus.auth.account.application;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public interface PasswordHasher {
    String hash(String rawPassword);
}
