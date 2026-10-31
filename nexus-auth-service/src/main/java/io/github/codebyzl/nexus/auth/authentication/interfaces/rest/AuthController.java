package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordResult;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordService;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutService;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenCommand;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenResult;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenService;
import io.github.codebyzl.nexus.auth.authentication.application.session.ListLoginSessionsQuery;
import io.github.codebyzl.nexus.auth.authentication.application.session.ListLoginSessionsService;
import io.github.codebyzl.nexus.auth.authentication.application.session.LoginSessionResult;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeAllSessionsQuery;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeAllSessionsService;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeLoginSessionCommand;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeLoginSessionService;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeOtherSessionsQuery;
import io.github.codebyzl.nexus.auth.authentication.application.session.RevokeOtherSessionsService;
import io.github.codebyzl.nexus.auth.infrastructure.security.proxy.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "登录、Token 轮换和退出接口")
public class AuthController {

    private final ClientIpResolver clientIpResolver;
    private final LoginWithPasswordService loginWithPasswordService;
    private final RotateRefreshTokenService rotateRefreshTokenService;
    private final LogoutService logoutService;
    private final ListLoginSessionsService listLoginSessionsService;
    private final RevokeLoginSessionService revokeLoginSessionService;
    private final RevokeOtherSessionsService revokeOtherSessionsService;
    private final RevokeAllSessionsService revokeAllSessionsService;

    @PostMapping("/login")
    @Operation(
            summary = "密码登录",
            description = "验证邮箱和密码，创建独立登录会话并返回 Token 对"
    )
    public LoginWithPasswordResponse login(
            @Valid
            @RequestBody
            LoginWithPasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LoginWithPasswordCommand command =
                new LoginWithPasswordCommand(
                        request.email(),
                        request.password(),
                        clientIpResolver.resolve(httpServletRequest)
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

    @GetMapping("/sessions")
    @Operation(
            summary = "查询当前账号登录会话",
            description = "返回当前账号的登录会话列表，并标识当前会话"
    )
    @SecurityRequirement(name = "bearerAuth")
    public List<LoginSessionResult> listSessions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long accountId = Long.valueOf(jwt.getSubject());

        UUID currentSessionId = UUID.fromString(
                jwt.getClaimAsString("sid")
        );

        ListLoginSessionsQuery query = new ListLoginSessionsQuery(
                accountId,
                currentSessionId
        );

        return listLoginSessionsService.getListSessions(query);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "撤销指定登录会话",
            description = "撤销当前账号拥有的指定 Session 及其活动 Refresh Token"
    )
    @SecurityRequirement(name = "bearerAuth")
    public void revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        RevokeLoginSessionCommand command =
                new RevokeLoginSessionCommand(
                        Long.valueOf(jwt.getSubject()),
                        sessionId
                );

        revokeLoginSessionService.revoke(command);
    }

    @DeleteMapping("/sessions/logout-others")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "退出其他设备",
            description = "保留当前 Session，撤销当前账号的其他活动 Session"
    )
    @SecurityRequirement(name = "bearerAuth")
    public void revokeOtherSessions(@AuthenticationPrincipal Jwt jwt) {
        long accountId = Long.parseLong(jwt.getSubject());
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));

        RevokeOtherSessionsQuery query = new RevokeOtherSessionsQuery(
                accountId,
                sessionId
        );

        revokeOtherSessionsService.revokeOthers(query);
    }

    @DeleteMapping("/sessions/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "退出全部设备",
            description = "撤销当前账号包括当前 Session 在内的全部活动 Session"
    )
    @SecurityRequirement(name = "bearerAuth")
    public void revokeAllSessions(@AuthenticationPrincipal Jwt jwt) {
        long accountId = Long.parseLong(jwt.getSubject());

        revokeAllSessionsService.revokeAll(
                new RevokeAllSessionsQuery(accountId)
        );
    }
}
