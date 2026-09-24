package io.github.codebyzl.nexus.auth.account.domain;


import java.util.Arrays;

public enum AccountStatus {

    ACTIVE(1),
    LOCKED(2),
    DISABLED(3);


    private final int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static AccountStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "未知账号状态：" + code
                        ));
    }
}