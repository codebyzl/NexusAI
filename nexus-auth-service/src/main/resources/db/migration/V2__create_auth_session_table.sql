CREATE TABLE `auth_session`
(
    `id`                 CHAR(36) NOT NULL COMMENT '登录会话 UUID',
    `account_id`         BIGINT NOT NULL COMMENT '账号 ID',
    `refresh_token_hash` CHAR(64)
                             CHARACTER SET ascii
                             COLLATE ascii_bin
                                  NOT NULL COMMENT 'Refresh Token SHA-256',
    `status`             TINYINT NOT NULL DEFAULT 1
        COMMENT '会话状态：1=ACTIVE；2=REVOKED',
    `expires_at`         DATETIME(6) NOT NULL
        COMMENT '会话与 Refresh Token 过期时间',
    `revoked_at`         DATETIME(6) NULL
        COMMENT '撤销时间',
    `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `updated_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_session_refresh_token_hash`
        (`refresh_token_hash`),
    KEY `idx_auth_session_account_status`
        (`account_id`, `status`),
    KEY `idx_auth_session_expires_at`
        (`expires_at`),

    CONSTRAINT `fk_auth_session_account`
        FOREIGN KEY (`account_id`)
            REFERENCES `account` (`id`),

    CONSTRAINT `chk_auth_session_status`
        CHECK (`status` IN (1, 2))
)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8mb4
    COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '账号登录会话';