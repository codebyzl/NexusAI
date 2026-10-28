package io.github.codebyzl.nexus.auth.infrastructure.security.ratelimit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
        prefix = "nexus.security.login-attempt"
)
public record LoginAttemptProperties(

        @Min(1)
        int maxFailures,

        @NotNull
        Duration window,

        @NotNull
        Duration blockDuration,

        @NotBlank
        String keyPrefix,

        @NotBlank
        String hmacSecret

) {
    public LoginAttemptProperties {
        if (window != null && !window.isPositive()) {
            throw new IllegalArgumentException(
                    "login-attempt.window must be positive"
            );
        }

        if (blockDuration != null && !blockDuration.isPositive()) {
            throw new IllegalArgumentException(
                    "login-attempt.block-duration must be positive"
            );
        }
    }
}
