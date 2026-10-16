package io.github.codebyzl.nexus.auth.account.interfaces.rest;



import io.github.codebyzl.nexus.auth.account.application.RegisterAccountCommand;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountResult;
import io.github.codebyzl.nexus.auth.account.application.RegisterAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountRegistrationController {

    private final RegisterAccountService registerAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterAccountResponse register(@Valid @RequestBody RegisterAccountRequest registerAccountRequest) {
        RegisterAccountCommand command = new RegisterAccountCommand(
                registerAccountRequest.email(),
                registerAccountRequest.password()
        );

        RegisterAccountResult result = registerAccountService.register(command);

        return new RegisterAccountResponse(
                result.accountId(),
                result.email()
        );
    }
}