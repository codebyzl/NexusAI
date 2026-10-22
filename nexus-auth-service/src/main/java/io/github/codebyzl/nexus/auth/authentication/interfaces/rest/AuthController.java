package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordResult;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordService;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LogoutService;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenCommand;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenResult;
import io.github.codebyzl.nexus.auth.authentication.application.RotateRefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginWithPasswordService loginWithPasswordService;
    private final RotateRefreshTokenService rotateRefreshTokenService;
    private final LogoutService logoutService;

    @PostMapping("/login")
    public LoginWithPasswordResponse login(@Valid @RequestBody LoginWithPasswordRequest request) {
        LoginWithPasswordCommand command = new LoginWithPasswordCommand(request.email(), request.password());

        LoginWithPasswordResult result = loginWithPasswordService.login(command);

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

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
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

    @PostMapping("/token/refresh")
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
}
