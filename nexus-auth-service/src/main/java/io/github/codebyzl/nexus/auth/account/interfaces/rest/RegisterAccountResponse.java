package io.github.codebyzl.nexus.auth.account.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "账号注册结果")
public record RegisterAccountResponse(
        @Schema(description = "账号 ID", example = "1")
        Long accountId,

        @Schema(description = "规范化后的邮箱", example = "user@example.com")
        String email
) {
}
