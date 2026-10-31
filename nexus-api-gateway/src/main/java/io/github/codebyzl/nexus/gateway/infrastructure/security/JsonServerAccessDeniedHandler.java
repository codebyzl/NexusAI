package io.github.codebyzl.nexus.gateway.infrastructure.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JsonServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final SecurityErrorResponseWriter writer;

    public JsonServerAccessDeniedHandler(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public Mono<Void> handle(
            ServerWebExchange exchange,
            AccessDeniedException exception
    ) {
        return writer.write(exchange, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "无权执行该操作");
    }
}
