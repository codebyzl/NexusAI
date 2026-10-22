package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @NotBlank(message = "Refresh Token 不能为空")
        @Size(
                max = 512,
                message = "Refresh Token 长度不合法"
        )
        String refreshToken
) {
}
