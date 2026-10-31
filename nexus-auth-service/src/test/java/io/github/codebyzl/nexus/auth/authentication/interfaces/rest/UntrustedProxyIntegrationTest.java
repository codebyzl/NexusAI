package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountCommand;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountService;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptContext;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptDecision;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptLimiter;
import io.github.codebyzl.nexus.auth.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 默认信任列表为空：真实连接不能靠自报 Header 成为可信代理。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "nexus.security.proxy.trusted-proxy-addresses=")
@Import(TestcontainersConfiguration.class)
class UntrustedProxyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RegisterAccountService registration;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginAttemptLimiter limiter;

    @Test
    void directLoginMustIgnoreSpoofedForwardingHeadersByDefault() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String password = "TestPassword123!";
        registration.register(new RegisterAccountCommand(email, password));
        when(limiter.check(any())).thenReturn(LoginAttemptDecision.permit());

        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Forwarded", "for=203.0.113.9;proto=https")
                .header("X-Forwarded-For", "203.0.113.9")
                .header("X-Real-IP", "203.0.113.9")
                .header("X-Nexus-Client-IP", "198.51.100.7")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Map.of("email", email, "password", password))))
                .build();
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build()) {
            assertThat(client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode())
                    .isEqualTo(200);
        }
        var context = ArgumentCaptor.forClass(LoginAttemptContext.class);
        verify(limiter).check(context.capture());
        assertThat(context.getValue().clientIp()).isEqualTo("127.0.0.1");
    }
}
