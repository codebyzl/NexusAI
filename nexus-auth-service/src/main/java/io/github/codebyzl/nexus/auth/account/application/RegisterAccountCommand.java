package io.github.codebyzl.nexus.auth.account.application;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record RegisterAccountCommand(String email, String rawPassword)
{
}