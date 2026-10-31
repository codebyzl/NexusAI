package io.github.codebyzl.nexus.gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/** 真 HTTP 网关 + 随机端口模拟下游，不需要数据库、Redis 或真实 AuthService。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    private static final List<ObservedRequest> REQUESTS = new CopyOnWriteArrayList<>();
    private static final List<org.springframework.http.HttpHeaders> HEADERS = new CopyOnWriteArrayList<>();
    private static final DisposableServer DOWNSTREAM = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .handle((request, response) -> {
                response.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                if (request.uri().equals("/api/auth/login")) {
                    response.status(429).header(HttpHeaders.RETRY_AFTER, "60");
                } else if (request.uri().equals("/api/auth/token/refresh")) {
                    response.status(401);
                } else if (request.uri().equals("/api/accounts")) {
                    response.status(201);
                }
                return response.sendString(request.receive().aggregate().asString()
                        .defaultIfEmpty("")
                        .map(body -> {
                            var headers = new org.springframework.http.HttpHeaders();
                            request.requestHeaders().forEach(entry ->
                                    headers.add(entry.getKey(), entry.getValue()));
                            HEADERS.add(headers);
                            REQUESTS.add(new ObservedRequest(
                                    request.method().name(), request.uri(),
                                    request.requestHeaders().get(HttpHeaders.AUTHORIZATION), body
                            ));
                            return "{\"source\":\"test-downstream\"}";
                        }));
            })
            .bindNow();

    @Autowired
    private WebTestClient client;

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        // 覆盖路由目标占位符，不覆盖 routes 列表的单个字段（列表不会跨配置源合并）。
        registry.add("AUTH_SERVICE_URI",
                () -> "http://127.0.0.1:" + DOWNSTREAM.port());
    }

    @AfterAll
    static void stopDownstream() {
        DOWNSTREAM.disposeNow();
    }

    @Test
    void shouldRejectMissingTokenAndIgnoreSpoofedIdentityHeaders() {
        int before = REQUESTS.size();
        expectUnauthorized(client.get().uri("/api/auth/sessions")
                .header("X-User-Id", "1")
                .header("X-Account-Id", "1")
                .exchange());
        assertThat(REQUESTS).hasSize(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "malformed", "wrong-signature", "wrong-algorithm", "expired",
            "future-nbf", "wrong-issuer", "wrong-type", "missing-type",
            "zero-sub", "invalid-sub", "missing-sub", "invalid-sid",
            "missing-sid", "missing-exp"
    })
    void shouldRejectInvalidAccessTokensBeforeForwarding(String mutation) {
        int before = REQUESTS.size();
        expectUnauthorized(client.get().uri("/api/auth/sessions")
                .headers(headers -> headers.setBearerAuth(token(mutation)))
                .exchange());
        assertThat(REQUESTS).hasSize(before);
    }

    @Test
    void shouldForwardValidTokenAndPreservePathQueryAndAuthorization() {
        String token = token("valid");
        int before = REQUESTS.size();
        client.get().uri("/api/auth/sessions?probe=1")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.source").isEqualTo("test-downstream");

        assertThat(REQUESTS).hasSize(before + 1);
        ObservedRequest observed = REQUESTS.get(before);
        assertThat(observed.method()).isEqualTo("GET");
        assertThat(observed.uri()).isEqualTo("/api/auth/sessions?probe=1");
        assertThat(observed.authorization()).isEqualTo("Bearer " + token);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/accounts", "/api/auth/login", "/api/auth/token/refresh", "/api/auth/logout"
    })
    void shouldAllowAnonymousPostAndPreserveDownstreamResponse(String path) {
        String body = "{\"probe\":\"unchanged\"}";
        int before = REQUESTS.size();
        int expectedStatus = switch (path) {
            case "/api/accounts" -> 201;
            case "/api/auth/login" -> 429;
            case "/api/auth/token/refresh" -> 401;
            default -> 200;
        };
        var result = client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
        if (path.equals("/api/auth/login")) {
            result.expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, "60");
        }
        result.expectBody().jsonPath("$.source").isEqualTo("test-downstream");
        assertThat(REQUESTS).hasSize(before + 1);
        assertThat(REQUESTS.get(before)).isEqualTo(new ObservedRequest("POST", path, null, body));
    }

    @Test
    void shouldNotAllowAnonymousAccessToSessionRevocation() {
        int before = REQUESTS.size();
        expectUnauthorized(client.delete()
                .uri("/api/auth/sessions/logout-all").exchange());
        assertThat(REQUESTS).hasSize(before);
    }

    @Test
    void shouldNotExtendPostWhitelistToOtherMethods() {
        int before = REQUESTS.size();
        expectUnauthorized(client.get().uri("/api/auth/login").exchange());
        assertThat(REQUESTS).hasSize(before);
    }

    @Test
    void shouldRejectInvalidBearerEvenOnAnonymousEndpoint() {
        int before = REQUESTS.size();
        expectUnauthorized(client.post().uri("/api/auth/login")
                .headers(headers -> headers.setBearerAuth("not-a-jwt"))
                .exchange());
        assertThat(REQUESTS).hasSize(before);
    }

    @Test
    void shouldReturnForbiddenForAuthenticatedRequestOutsideAllowedPaths() {
        int before = REQUESTS.size();
        client.get().uri("/api/not-configured")
                .headers(headers -> headers.setBearerAuth(token("valid")))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FORBIDDEN")
                .jsonPath("$.message").isEqualTo("无权执行该操作");
        assertThat(REQUESTS).hasSize(before);
    }

    @Test
    void shouldReplaceSpoofedProxyHeadersAndStripClientIdentityBeforeForwarding() {
        int before = REQUESTS.size();
        String bearer = token("valid");
        client.get().uri("/api/auth/sessions")
                .headers(headers -> headers.setBearerAuth(bearer))
                .header("Forwarded", "for=198.51.100.20;host=evil.example;proto=https")
                .header("X-Forwarded-For", "198.51.100.20, 203.0.113.7")
                .header("X-Forwarded-Host", "evil.example")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Prefix", "/evil")
                .header("X-Real-IP", "198.51.100.20")
                .header("X-Nexus-Client-IP", "198.51.100.20", "203.0.113.7")
                .header("X-Nexus-Account-Id", "999")
                .header("X-User-Id", "999")
                .header("X-Account-Id", "999")
                .header("X-Session-Id", "fake-session")
                .header("X-Authenticated-User", "admin")
                .header("X-Authenticated-User-Id", "999")
                .header("X-Auth-Request-User", "admin")
                .header("X-Auth-Request-Email", "admin@example.com")
                .exchange()
                .expectStatus().isOk();

        assertThat(REQUESTS).hasSize(before + 1);
        var received = HEADERS.get(before);
        assertThat(received.get("X-Nexus-Client-IP")).containsExactly("127.0.0.1");
        assertThat(received.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + bearer);
        assertThat(received.keySet()).noneMatch(name -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            return lower.equals("forwarded") || lower.startsWith("x-forwarded-")
                    || lower.equals("x-real-ip") || lower.equals("x-nexus-account-id")
                    || lower.equals("x-user-id") || lower.equals("x-account-id")
                    || lower.equals("x-session-id") || lower.startsWith("x-authenticated-")
                    || lower.startsWith("x-auth-request-");
        });
        assertThat(REQUESTS.get(before).uri()).isEqualTo("/api/auth/sessions");
    }

    @Test
    void shouldNotPersistAuthenticationInWebSession() {
        client.get().uri("/api/auth/sessions")
                .headers(headers -> headers.setBearerAuth(token("valid")))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist(HttpHeaders.SET_COOKIE);
        expectUnauthorized(client.get().uri("/api/auth/sessions").exchange());
    }

    private static void expectUnauthorized(WebTestClient.ResponseSpec response) {
        response.expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_ACCESS_TOKEN")
                .jsonPath("$.message").isEqualTo("Access Token 无效或已过期");
    }

    private static String token(String mutation) {
        if (mutation.equals("malformed")) {
            return "not-a-jwt";
        }
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("nexus-auth-service-test")
                .subject("1")
                .issuedAt(now.minusSeconds(1200))
                .expiresAt(now.plusSeconds(300))
                .claim("token_type", "access")
                .claim("sid", UUID.randomUUID().toString());
        switch (mutation) {
            case "expired" -> claims.expiresAt(now.minusSeconds(600));
            case "future-nbf" -> claims.notBefore(now.plusSeconds(600));
            case "wrong-issuer" -> claims.issuer("untrusted-issuer");
            case "wrong-type" -> claims.claim("token_type", "refresh");
            case "zero-sub" -> claims.subject("0");
            case "invalid-sub" -> claims.subject("not-an-id");
            case "invalid-sid" -> claims.claim("sid", "not-a-uuid");
            case "missing-type" -> claims.claims(values -> values.remove("token_type"));
            case "missing-sub" -> claims.claims(values -> values.remove("sub"));
            case "missing-sid" -> claims.claims(values -> values.remove("sid"));
            case "missing-exp" -> claims.claims(values -> values.remove("exp"));
            default -> { }
        }
        byte[] key = new byte[mutation.equals("wrong-algorithm") ? 64 : 32];
        if (mutation.equals("wrong-signature")) {
            Arrays.fill(key, (byte) 1);
        }
        MacAlgorithm algorithm = mutation.equals("wrong-algorithm")
                ? MacAlgorithm.HS384 : MacAlgorithm.HS256;
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(algorithm).build(), claims.build()
        )).getTokenValue();
    }

    private record ObservedRequest(String method, String uri, String authorization, String body) {
    }
}
