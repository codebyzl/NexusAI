package io.github.codebyzl.nexus.auth.account.interfaces.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record RegisterAccountRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}