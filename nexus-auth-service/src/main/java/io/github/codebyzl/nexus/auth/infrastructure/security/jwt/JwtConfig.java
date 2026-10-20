package io.github.codebyzl.nexus.auth.infrastructure.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
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

        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(secretKey)
                .algorithm(JWSAlgorithm.HS256)
                .build();

        return new NimbusJwtEncoder(
                new ImmutableJWKSet<SecurityContext>(
                        new JWKSet(jwk)
                )
        );
    }
}
