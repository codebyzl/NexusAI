package io.github.codebyzl.nexus.auth.infrastructure.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {

        SecretKey secretKey = getSecretKey(properties);

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(
                        new JWKSet(jwk)
                )
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {

        SecretKey secretKey = getSecretKey(properties);

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(
                                properties.issuer()
                        ),
                        customValidator
                )
        );

        return decoder;
    }

    private static SecretKey getSecretKey(JwtProperties properties) {
        byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(properties.secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_ACCESS_TOKEN_SECRET 必须是合法的 Base64",
                    exception
            );
        }

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT Access Token 密钥至少需要 256 bit"
            );
        }

        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    private final OAuth2TokenValidator<Jwt> customValidator = jwt -> {
        try {
            // 1. 必须是 Access Token
            if (!"access".equals(
                    jwt.getClaimAsString("token_type")
            )) {
                throw new IllegalArgumentException();
            }

            // 2. sub 必须是有效账号 ID
            long accountId = Long.parseLong(jwt.getSubject());

            if (accountId <= 0) {
                throw new IllegalArgumentException();
            }

            // 3. sid 必须是合法 UUID
            UUID.fromString(
                    jwt.getClaimAsString("sid")
            );

            // 4. exp 必须存在
            if (jwt.getExpiresAt() == null) {
                throw new IllegalArgumentException();
            }

            return OAuth2TokenValidatorResult.success();

        } catch (RuntimeException e) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            "invalid_token",
                            "Invalid access token claims",
                            null
                    )
            );
        }
    };

}
