package io.github.codebyzl.nexus.auth.infrastructure.security.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "nexus.security.proxy")
public record TrustedProxyProperties(List<String> trustedProxyAddresses) {

    public TrustedProxyProperties {
        trustedProxyAddresses = trustedProxyAddresses == null
                ? List.of() : List.copyOf(trustedProxyAddresses);
        for (String address : trustedProxyAddresses) {
            if (IpLiteral.parse(address).isEmpty()) {
                throw new IllegalArgumentException(
                        "trusted-proxy-addresses 必须使用精确 IP 字面量，不接受主机名、CIDR 或端口"
                );
            }
        }
    }
}
