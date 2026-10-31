package io.github.codebyzl.nexus.gateway.infrastructure.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final SecurityErrorResponseWriter writer;

    public JsonServerAuthenticationEntryPoint(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception
    ) {
        // 沿用 AuthService 契约，不对外暴露签名或 Claims 校验细节。
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return writer.write(exchange, HttpStatus.UNAUTHORIZED,
                "INVALID_ACCESS_TOKEN", "Access Token 无效或已过期");
    }
}
