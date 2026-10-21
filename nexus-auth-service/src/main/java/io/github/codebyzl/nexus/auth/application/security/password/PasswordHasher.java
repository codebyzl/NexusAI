package io.github.codebyzl.nexus.auth.application.security.password;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
