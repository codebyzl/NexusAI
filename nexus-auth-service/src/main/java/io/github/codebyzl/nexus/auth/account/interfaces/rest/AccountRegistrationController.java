package io.github.codebyzl.nexus.auth.account.interfaces.rest;

import io.github.codebyzl.nexus.auth.account.application.RegisterAccountCommand;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountResult;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountService;
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
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "账号注册接口")
public class AccountRegistrationController {

    private final RegisterAccountService registerAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "注册账号",
            description = "使用邮箱和密码创建一个新账号"
    )
    public RegisterAccountResponse register(
            @Valid
            @RequestBody
            RegisterAccountRequest request
    ) {
        RegisterAccountCommand command =
                new RegisterAccountCommand(
                        request.email(),
                        request.password()
                );

        RegisterAccountResult result =
                registerAccountService.register(command);

        return new RegisterAccountResponse(
                result.accountId(),
                result.email()
        );
    }
}
