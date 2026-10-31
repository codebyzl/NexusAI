package io.github.codebyzl.nexus.auth.infrastructure.security.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientIpResolverTest {

    @Test
    void shouldTrustNoProxyByDefault() {
        var resolver = new ClientIpResolver(new TrustedProxyProperties(null));
        var request = request("127.0.0.1", "198.51.100.7");
        assertThat(resolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldIgnoreEveryForwardingHeaderFromUntrustedPeer() {
        var request = request("192.0.2.9", "198.51.100.7");
        request.addHeader("Forwarded", "for=203.0.113.9");
        request.addHeader("X-Forwarded-For", "203.0.113.9");
        request.addHeader("X-Real-IP", "203.0.113.9");
        assertThat(resolver().resolve(request)).isEqualTo("192.0.2.9");
    }

    @Test
    void shouldAcceptSingleLiteralOnlyFromConfiguredPeer() {
        assertThat(resolver().resolve(request("127.0.0.1", "198.51.100.7")))
                .isEqualTo("198.51.100.7");
    }

    @Test
    void shouldNormalizeIpv6AndMatchTrustedPeerByAddressNotSpelling() {
        var resolver = new ClientIpResolver(new TrustedProxyProperties(List.of("::1")));
        String normalized = resolver.resolve(request("0:0:0:0:0:0:0:1", "2001:db8::7"));
        assertThat(IpLiteral.parse(normalized)).isEqualTo(IpLiteral.parse("2001:db8::7"));
    }

    @Test
    void shouldRejectRepeatedHeaderEvenIfValuesAreIdentical() {
        var request = request("127.0.0.1", "198.51.100.7");
        request.addHeader(ClientIpResolver.CLIENT_IP_HEADER, "198.51.100.7");
        assertThat(resolver().resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldFallBackWhenTrustedPeerProvidesNoCustomHeader() {
        var request = request("127.0.0.1", null);
        request.addHeader("X-Forwarded-For", "198.51.100.7");
        assertThat(resolver().resolve(request)).isEqualTo("127.0.0.1");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "198.51.100.7,203.0.113.8", "198.51.100.7:8080",
            "localhost", "example.com", "999.1.1.1", "127.1", "2130706433",
            "01.2.3.4", " 198.51.100.7", "198.51.100.7 ",
            "[2001:db8::7]", "fe80::1%eth0", "not-an-ip", "1:2:3"
    })
    void shouldRejectMalformedOrAmbiguousClientAddresses(String value) {
        assertThat(resolver().resolve(request("127.0.0.1", value))).isEqualTo("127.0.0.1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"localhost", "127.0.0.0/8", "127.0.0.1:8080", "", "*"})
    void shouldFailConfigurationForNonLiteralTrustedProxy(String value) {
        assertThatThrownBy(() -> new TrustedProxyProperties(List.of(value)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ClientIpResolver resolver() {
        return new ClientIpResolver(new TrustedProxyProperties(List.of("127.0.0.1")));
    }

    private static MockHttpServletRequest request(String remote, String header) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remote);
        if (header != null) {
            request.addHeader(ClientIpResolver.CLIENT_IP_HEADER, header);
        }
        return request;
    }
}
