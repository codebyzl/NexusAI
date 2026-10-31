package io.github.codebyzl.nexus.gateway.infrastructure.security.jwt;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nexus.security.jwt")
public record GatewayJwtProperties(
        @NotBlank String issuer,
        @NotBlank String secret
) {
}
