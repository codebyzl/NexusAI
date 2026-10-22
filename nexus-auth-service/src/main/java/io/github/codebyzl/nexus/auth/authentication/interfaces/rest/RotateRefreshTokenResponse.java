package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Refresh Token 轮换结果")
public record RotateRefreshTokenResponse(
        @Schema(description = "新签发的 JWT Access Token")
        String accessToken,

        @Schema(description = "Token 类型", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 过期时间")
        Instant accessTokenExpiresAt,

        @Schema(description = "轮换后新的不透明 Refresh Token")
        String refreshToken,

        @Schema(description = "Refresh Token 和 Session 的绝对过期时间")
        Instant refreshTokenExpiresAt
) {
}
