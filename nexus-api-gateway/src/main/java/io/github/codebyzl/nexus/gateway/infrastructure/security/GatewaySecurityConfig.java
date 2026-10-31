package io.github.codebyzl.nexus.gateway.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JsonServerAuthenticationEntryPoint entryPoint,
            JsonServerAccessDeniedHandler deniedHandler
    ) {
        return http
                // 当前入口仅使用显式 Bearer Header，不使用 Cookie 自动认证。
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .requestCache(cache -> cache.requestCache(NoOpServerRequestCache.getInstance()))
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.POST,
                                "/api/accounts",
                                "/api/auth/login",
                                "/api/auth/token/refresh",
                                "/api/auth/logout"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/actuator/health", "/actuator/health/**"
                        ).permitAll()
                        .pathMatchers("/api/auth/**").authenticated()
                        .anyExchange().denyAll()
                )
                // 无凭据访问保护资源，以及已认证但无权限。
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                )
                // 解析/校验 Bearer Token 失败发生在认证阶段，也接入同一错误契约。
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> { })
                )
                .build();
    }
}
