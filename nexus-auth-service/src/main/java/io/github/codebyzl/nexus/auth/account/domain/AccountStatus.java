package io.github.codebyzl.nexus.auth.account.domain;

import com.baomidou.mybatisplus.annotation.EnumValue;


public enum AccountStatus {

    ACTIVE(1),
    LOCKED(2),
    DISABLED(3);

    @EnumValue
    private final int code;

    AccountStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}