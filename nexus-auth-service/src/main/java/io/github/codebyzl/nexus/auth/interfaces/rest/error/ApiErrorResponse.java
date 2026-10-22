package io.github.codebyzl.nexus.auth.interfaces.rest.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一业务错误响应")
public record ApiErrorResponse(
        @Schema(
                description = "稳定的英文业务错误码",
                example = "INVALID_REQUEST"
        )
        String code,

        @Schema(
                description = "面向调用方的错误信息",
                example = "请求参数不合法"
        )
        String message
) {
}
