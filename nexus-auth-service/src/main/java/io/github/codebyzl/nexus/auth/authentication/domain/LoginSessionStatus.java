package io.github.codebyzl.nexus.auth.authentication.domain;


import java.util.Arrays;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
public enum LoginSessionStatus {
    ACTIVE(1),    // 有效
    REVOKED(2);   // 已撤销

    private final int code;

    LoginSessionStatus(int code) {
        this.code = code;
    }

    public static LoginSessionStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "未知登录会话状态：" + code
                        ));
    }

    public int getCode() {
        return code;
    }
}