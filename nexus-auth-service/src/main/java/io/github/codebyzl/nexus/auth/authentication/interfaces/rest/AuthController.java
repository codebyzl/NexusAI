package io.github.codebyzl.nexus.auth.authentication.interfaces.rest;

import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordCommand;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordResult;
import io.github.codebyzl.nexus.auth.authentication.application.LoginWithPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
