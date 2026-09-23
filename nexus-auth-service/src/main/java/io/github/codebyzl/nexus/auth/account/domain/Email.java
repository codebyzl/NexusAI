package io.github.codebyzl.nexus.auth.account.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        if (value == null) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 254 || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}