package io.github.codebyzl.nexus.auth.application.security.ratelimit;

public record LoginAttemptContext(
        String normalizedEmail,
        String clientIp
) {
}