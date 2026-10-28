package io.github.codebyzl.nexus.auth.authentication.application;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public record LoginWithPasswordCommand(String email, String rawPassword, String clientIp)
{
}