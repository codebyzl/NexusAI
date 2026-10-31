package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptContext;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptDecision;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptLimiter;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.LoginSessionStatus;
import io.github.codebyzl.nexus.auth.infrastructure.security.proxy.ClientIpResolver;
import io.github.codebyzl.nexus.auth.support.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 真实 HTTP/Tomcat + MySQL + JWT/Session；仅替换限流端口，不宣称验证 Redis 算法。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nexus.security.proxy.trusted-proxy-addresses=127.0.0.1")
@Import(TestcontainersConfiguration.class)
class SessionSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private LoginSessionRepository sessions;

    @MockitoBean
    private LoginAttemptLimiter limiter;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @AfterEach
    void closeHttpClient() {
        client.close();
    }

    @BeforeEach
    void allowLogin() {
        when(limiter.check(any())).thenReturn(LoginAttemptDecision.permit());
    }

    @Test
    void directRequestsCannotReplaceBearerWithIdentityOrProxyHeaders() throws Exception {
        var response = send("GET", "/auth/sessions", null, null, Map.of(
                "X-User-Id", "1", "X-Account-Id", "1",
                ClientIpResolver.CLIENT_IP_HEADER, "198.51.100.7",
                "Forwarded", "for=198.51.100.7;proto=https",
                "X-Forwarded-For", "198.51.100.7"
        ));
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(json(response).path("code").asText()).isEqualTo("INVALID_ACCESS_TOKEN");
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
    }

    @Test
    void logoutMustInvalidateAnOtherwiseCryptographicallyValidAccessToken() throws Exception {
        Tokens tokens = registerAndLogin();
        assertThat(send("GET", "/auth/sessions", tokens.access(), null, Map.of()).statusCode())
                .isEqualTo(200);

        var logout = send("POST", "/auth/logout", null,
                objectMapper.writeValueAsString(Map.of("refreshToken", tokens.refresh())), Map.of());
        assertThat(logout.statusCode()).isEqualTo(204);
        // JWT 仍未过期且签名有效；拒绝原因必须来自 Session 撤销，而非时间到期。
        assertThat(jwtDecoder.decode(tokens.access()).getExpiresAt()).isAfter(Instant.now());
        assertThat(sessions.findById(tokens.session()).orElseThrow().getStatus())
                .isEqualTo(LoginSessionStatus.REVOKED);
        var denied = send("GET", "/auth/sessions", tokens.access(), null, Map.of());
        assertThat(denied.statusCode()).isEqualTo(401);
        assertThat(json(denied).path("code").asText()).isEqualTo("INVALID_ACCESS_TOKEN");
    }

    @Test
    void accountACannotRevokeAccountBSessionOrImpersonateBWithHeaders() throws Exception {
        Tokens a = registerAndLogin();
        Tokens b = registerAndLogin();
        var denied = send("DELETE", "/auth/sessions/" + b.session(), a.access(), null, Map.of(
                "X-User-Id", b.accountId(), "X-Account-Id", b.accountId(),
                "X-Session-Id", b.session().toString()
        ));
        assertThat(denied.statusCode()).isEqualTo(404);
        assertThat(json(denied).path("code").asText()).isEqualTo("SESSION_NOT_FOUND");
        assertThat(sessions.findById(b.session()).orElseThrow().getStatus())
                .isEqualTo(LoginSessionStatus.ACTIVE);
        assertThat(send("GET", "/auth/sessions", b.access(), null, Map.of()).statusCode())
                .isEqualTo(200);

        // 拒绝跨账号操作没有误伤 A；A 仍可以撤销自己，B 仍有效。
        assertThat(send("DELETE", "/auth/sessions/" + a.session(), a.access(), null, Map.of())
                .statusCode()).isEqualTo(204);
        assertThat(send("GET", "/auth/sessions", a.access(), null, Map.of()).statusCode())
                .isEqualTo(401);
        assertThat(send("GET", "/auth/sessions", b.access(), null, Map.of()).statusCode())
                .isEqualTo(200);
    }

    @Test
    void verifiedProxyClientIpMustReachLoginLimiterWithoutAutomaticForwardedRewriting() throws Exception {
        String email = register();
        var response = send("POST", "/auth/login", null, credentials(email), Map.of(
                ClientIpResolver.CLIENT_IP_HEADER, "198.51.100.7",
                "Forwarded", "for=203.0.113.9;proto=https",
                "X-Forwarded-For", "203.0.113.9"
        ));
        assertThat(response.statusCode()).isEqualTo(200);
        var context = ArgumentCaptor.forClass(LoginAttemptContext.class);
        verify(limiter).check(context.capture());
        assertThat(context.getValue().clientIp()).isEqualTo("198.51.100.7");
        assertThat(context.getValue().normalizedEmail()).isEqualTo(email);
    }

    private Tokens registerAndLogin() throws Exception {
        String email = register();
        var response = send("POST", "/auth/login", null, credentials(email), Map.of());
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode body = json(response);
        String access = body.path("accessToken").asText();
        return new Tokens(access, body.path("refreshToken").asText(),
                UUID.fromString(jwtDecoder.decode(access).getClaimAsString("sid")),
                body.path("accountId").asText());
    }

    private String register() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        var response = send("POST", "/accounts", null, credentials(email), Map.of());
        assertThat(response.statusCode()).isEqualTo(201);
        return email;
    }

    private String credentials(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "email", email, "password", "TestPassword123!"
        ));
    }

    private HttpResponse<String> send(
            String method, String path, String token, String body, Map<String, String> headers
    ) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path))
                .timeout(Duration.ofSeconds(10))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        headers.forEach(request::header);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private record Tokens(String access, String refresh, UUID session, String accountId) {
    }
}
