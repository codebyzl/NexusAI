package io.github.codebyzl.nexus.auth.infrastructure.security;

import io.github.codebyzl.nexus.auth.authentication.domain.LoginSession;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final LoginSessionRepository loginSessionRepository;

    private final JwtAuthenticationConverter delegate =
            new JwtAuthenticationConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Long accountId;
        UUID sessionId;

        try {
            accountId = Long.valueOf(jwt.getSubject());
            sessionId = UUID.fromString(
                    jwt.getClaimAsString("sid")
            );
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidBearerTokenException(
                    "Invalid access token claims"
            );
        }

        LoginSession session = loginSessionRepository
                .findById(sessionId)
                .orElseThrow(() ->
                        new InvalidBearerTokenException(
                                "Login session is invalid"
                        )
                );

        if (!session.getAccountId().equals(accountId)
                || !session.isActiveAt(Instant.now())) {
            throw new InvalidBearerTokenException(
                    "Login session is invalid"
            );
        }

        return delegate.convert(jwt);
    }
}
