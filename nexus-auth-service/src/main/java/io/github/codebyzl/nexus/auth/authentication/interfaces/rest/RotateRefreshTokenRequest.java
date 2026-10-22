package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Refresh Token 轮换请求")
public record RotateRefreshTokenRequest(
        @NotBlank(message = "Refresh Token 不能为空")
        @Size(
                max = 512,
                message = "Refresh Token 长度不合法"
        )
        @Schema(
                description = "当前客户端持有的 Refresh Token",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String refreshToken
) {
}
