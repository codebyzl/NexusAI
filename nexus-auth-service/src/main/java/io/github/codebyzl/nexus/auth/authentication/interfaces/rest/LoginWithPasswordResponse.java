package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "密码登录结果")
public record LoginWithPasswordResponse(
        @Schema(description = "账号 ID", example = "1")
        Long accountId,

        @Schema(description = "账号邮箱", example = "user@example.com")
        String email,

        @Schema(description = "短期 JWT Access Token")
        String accessToken,

        @Schema(description = "Token 类型", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 过期时间")
        Instant accessTokenExpiresAt,

        @Schema(description = "不透明 Refresh Token")
        String refreshToken,

        @Schema(description = "Refresh Token 和 Session 的绝对过期时间")
        Instant refreshTokenExpiresAt
) {
}
