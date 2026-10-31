package io.github.codebyzl.nexus.gateway.infrastructure.security;

import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Set;

/** 单层入口：只使用 TCP 连接对端，不解析客户端提供的代理链。 */
@Component
public class TrustedClientHeadersFilter implements HttpHeadersFilter, Ordered {

    public static final String CLIENT_IP_HEADER = "X-Nexus-Client-IP";

    private static final Set<String> IDENTITY_HEADERS = Set.of(
            "x-user-id", "x-account-id", "x-session-id", "x-authenticated-user",
            "x-authenticated-user-id", "x-auth-request-user", "x-auth-request-email"
    );

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        HttpHeaders output = new HttpHeaders();
        input.forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!lower.equals("forwarded")
                    && !lower.startsWith("x-forwarded-")
                    && !lower.equals("x-real-ip")
                    && !lower.startsWith("x-nexus-")
                    && !IDENTITY_HEADERS.contains(lower)) {
                output.addAll(name, values);
            }
        });

        InetSocketAddress peer = exchange.getRequest().getRemoteAddress();
        if (peer != null && peer.getAddress() != null) {
            String address = peer.getAddress().getHostAddress();
            // TCP 对端的 IPv6 scope 仅有本地意义；不把 zone ID 跨服务传递。
            int scope = address.indexOf('%');
            output.set(CLIENT_IP_HEADER, scope < 0 ? address : address.substring(0, scope));
        }
        return output;
    }

    @Override
    public boolean supports(Type type) {
        return type == Type.REQUEST;
    }

    @Override
    public int getOrder() {
        // 在请求头过滤链末尾覆盖来源信息，不让其他默认转发过滤器重新追加伪造头。
        return Ordered.LOWEST_PRECEDENCE;
    }
}
