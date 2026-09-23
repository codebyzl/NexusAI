create table `account`
(
    id BIGINT NOT NULL AUTO_INCREMENT  COMMENT '账户id',
    email VARCHAR(254) NOT NULL  COMMENT '电子邮件',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码Hash',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账户状态1 = ACTIVE； 2 = LOCKED； 3 = DISABLED；',
    email_verified_at DATETIME(6) NULL COMMENT '邮箱验证时间',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_email` (`email`),
    CONSTRAINT `chk_account_status`
        CHECK (`status` IN (1, 2, 3))
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '认证账号表';