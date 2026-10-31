package io.github.codebyzl.nexus.gateway.infrastructure.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/** 只写网关自身的安全错误；不改写下游服务返回的业务错误。 */
@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message
    ) {
        return Mono.defer(() -> {
            byte[] body;
            try {
                body = objectMapper.writeValueAsBytes(Map.of(
                        "code", code,
                        "message", message
                ));
            } catch (JsonProcessingException exception) {
                return Mono.error(exception);
            }
            var response = exchange.getResponse();
            response.setStatusCode(status);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        });
    }
}
