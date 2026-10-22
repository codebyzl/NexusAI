package io.github.codebyzl.nexus.auth.account.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "账号注册请求")
public record RegisterAccountRequest(
        @NotBlank
        @Email
        @Schema(
                description = "登录邮箱",
                example = "user@example.com"
        )
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(
                description = "登录密码，长度为 8～72 个字符",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        String password
) {
}
