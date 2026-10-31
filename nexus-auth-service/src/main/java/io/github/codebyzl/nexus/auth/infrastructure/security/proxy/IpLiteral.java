package io.github.codebyzl.nexus.auth.infrastructure.security.proxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

/** 仅接受无端口、无 zone、无代理链的 IP 字面量；不解析主机名或执行 DNS 查询。 */
final class IpLiteral {

    private static final Pattern IPV4 = Pattern.compile(
            "(?:0|[1-9][0-9]{0,2})(?:\\.(?:0|[1-9][0-9]{0,2})){3}"
    );
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9a-fA-F:.]+");

    private IpLiteral() {
    }

    static Optional<InetAddress> parse(String value) {
        if (value == null || value.isEmpty() || value.length() > 45) {
            return Optional.empty();
        }
        try {
            if (IPV4.matcher(value).matches()) {
                byte[] bytes = new byte[4];
                String[] parts = value.split("\\.");
                for (int i = 0; i < parts.length; i++) {
                    int part = Integer.parseInt(parts[i]);
                    if (part > 255) {
                        return Optional.empty();
                    }
                    bytes[i] = (byte) part;
                }
                return Optional.of(InetAddress.getByAddress(bytes));
            }
            if (value.indexOf(':') >= 0 && IPV6_CHARACTERS.matcher(value).matches()) {
                // 经字符集与冒号约束后只可能是 IPv6 字面量，不可能是 DNS 主机名。
                return Optional.of(InetAddress.getByName(value));
            }
        } catch (UnknownHostException | IllegalArgumentException exception) {
            return Optional.empty();
        }
        return Optional.empty();
    }
}
