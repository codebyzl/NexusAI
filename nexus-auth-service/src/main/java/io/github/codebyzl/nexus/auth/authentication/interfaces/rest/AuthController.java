package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordResult;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordService;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutService;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenCommand;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenResult;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "登录、Token 轮换和退出接口")
public class AuthController {

    private final LoginWithPasswordService loginWithPasswordService;
    private final RotateRefreshTokenService rotateRefreshTokenService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    @Operation(
            summary = "密码登录",
            description = "验证邮箱和密码，创建独立登录会话并返回 Token 对"
    )
    public LoginWithPasswordResponse login(
            @Valid
            @RequestBody
            LoginWithPasswordRequest request
    ) {
        LoginWithPasswordCommand command =
                new LoginWithPasswordCommand(
                        request.email(),
                        request.password()
                );

        LoginWithPasswordResult result =
                loginWithPasswordService.login(command);

        return new LoginWithPasswordResponse(
                result.accountId(),
                result.email(),
                result.accessToken(),
                "Bearer",
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt()
        );
    }

    @PostMapping("/token/refresh")
    @Operation(
            summary = "轮换 Token",
            description = "使用当前 Refresh Token 获取新的 Access Token 和 Refresh Token"
    )
    public RotateRefreshTokenResponse refresh(
            @Valid
            @RequestBody
            RotateRefreshTokenRequest request
    ) {
        RotateRefreshTokenCommand command =
                new RotateRefreshTokenCommand(
                        request.refreshToken()
                );

        RotateRefreshTokenResult result =
                rotateRefreshTokenService.rotate(command);

        return new RotateRefreshTokenResponse(
                result.accessToken(),
                "Bearer",
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "退出当前会话",
            description = "根据 Refresh Token 撤销所属 Session 和当前活动 Token"
    )
    public void logout(
            @Valid
            @RequestBody
            LogoutRequest request
    ) {
        LogoutCommand command =
                new LogoutCommand(
                        request.refreshToken()
                );

        logoutService.logout(command);
    }
}
