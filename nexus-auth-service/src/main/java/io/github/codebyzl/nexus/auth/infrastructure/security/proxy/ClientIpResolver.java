package io.github.codebyzl.nexus.auth.infrastructure.security.proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 仅用于来源 IP/限流；账号身份始终来自经过验证的 JWT。 */
@Component
public class ClientIpResolver {

    public static final String CLIENT_IP_HEADER = "X-Nexus-Client-IP";

    private final Set<InetAddress> trustedProxies;

    public ClientIpResolver(TrustedProxyProperties properties) {
        trustedProxies = properties.trustedProxyAddresses().stream()
                .map(address -> IpLiteral.parse(address).orElseThrow())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String rawPeer = request.getRemoteAddr();
        var peer = IpLiteral.parse(rawPeer);
        String fallback = peer.map(InetAddress::getHostAddress).orElse(rawPeer);
        if (peer.isEmpty() || !trustedProxies.contains(peer.get())) {
            return fallback;
        }

        var enumeration = request.getHeaders(CLIENT_IP_HEADER);
        List<String> values = enumeration == null ? List.of() : Collections.list(enumeration);
        // 拒绝重复头、逗号链、主机名、端口、空白和非法值；不猜哪一项可信。
        if (values.size() != 1) {
            return fallback;
        }
        return IpLiteral.parse(values.getFirst())
                .map(InetAddress::getHostAddress)
                .orElse(fallback);
    }
}
