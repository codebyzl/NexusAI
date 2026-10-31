package io.github.codebyzl.nexus.gateway.infrastructure.security.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewayJwtConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(GatewayJwtProperties properties) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_ACCESS_TOKEN_SECRET 必须是合法的 Base64", exception
            );
        }
        if (secret.length < 32) {
            throw new IllegalStateException("JWT Access Token 密钥至少需要 256 bit");
        }

        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
                .withSecretKey(new SecretKeySpec(secret, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // 与 AuthService 的契约一致：默认时间/issuer 校验 + Access Token 必要 Claims。
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()),
                accessTokenClaimsValidator()
        ));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> accessTokenClaimsValidator() {
        return jwt -> {
            try {
                if (!"access".equals(jwt.getClaimAsString("token_type"))
                        || Long.parseLong(jwt.getSubject()) <= 0
                        || jwt.getExpiresAt() == null) {
                    throw new IllegalArgumentException();
                }
                UUID.fromString(jwt.getClaimAsString("sid"));
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException exception) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Invalid access token claims", null
                ));
            }
        };
    }
}
