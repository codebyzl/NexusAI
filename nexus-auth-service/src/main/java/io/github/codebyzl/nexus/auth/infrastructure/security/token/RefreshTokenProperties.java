package io.github.codebyzl.nexus.auth.infrastructure.security.token;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Validated
@ConfigurationProperties(prefix = "nexus.security.refresh-token")
public record RefreshTokenProperties(
        @NotNull Duration ttl,
        @Min(32) int byteLength
) {
}
