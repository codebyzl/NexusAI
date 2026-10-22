package io.github.codebyzl.nexus.auth.infrastructure.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    public static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI nexusAuthOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("NexusAI Auth Service API")
                                .description(
                                        "账号注册、密码登录、Token 轮换和会话撤销接口"
                                )
                                .version("0.1.0")
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH_SCHEME,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .description(
                                                        "在需要认证的接口中提交 Access Token"
                                                )
                                )
                );
    }
}
