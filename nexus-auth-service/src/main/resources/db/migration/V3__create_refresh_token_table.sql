CREATE TABLE `refresh_token`
(
    `id`                   CHAR(36) NOT NULL
        COMMENT 'Refresh Token UUID',
    `session_id`           CHAR(36) NOT NULL
        COMMENT '所属登录会话，也是 Token Family ID',
    `token_hash`           CHAR(64)
                               CHARACTER SET ascii
                               COLLATE ascii_bin
                                    NOT NULL COMMENT 'Refresh Token SHA-256',
    `status`               TINYINT NOT NULL
        COMMENT '1=ACTIVE；2=USED；3=REVOKED',
    `expires_at`           DATETIME(6) NOT NULL,
    `created_at`           DATETIME(6) NOT NULL,
    `used_at`              DATETIME(6) NULL,
    `revoked_at`           DATETIME(6) NULL,
    `replaced_by_token_id` CHAR(36) NULL
        COMMENT '轮换后继 Token ID',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    UNIQUE KEY `uk_refresh_token_replaced_by`
        (`replaced_by_token_id`),
    KEY `idx_refresh_token_session_status`
        (`session_id`, `status`),
    KEY `idx_refresh_token_expires_at`
        (`expires_at`),

    CONSTRAINT `fk_refresh_token_session`
        FOREIGN KEY (`session_id`)
            REFERENCES `auth_session` (`id`)
            ON DELETE CASCADE,

    CONSTRAINT `fk_refresh_token_replaced_by`
        FOREIGN KEY (`replaced_by_token_id`)
            REFERENCES `refresh_token` (`id`)
            ON DELETE SET NULL,

    CONSTRAINT `chk_refresh_token_status`
        CHECK (`status` IN (1, 2, 3))
)
    COMMENT = 'Refresh Token 历史和轮换关系';

INSERT INTO `refresh_token`
(
    `id`,
    `session_id`,
    `token_hash`,
    `status`,
    `expires_at`,
    `created_at`,
    `revoked_at`
)
SELECT
    UUID(),
    `id`,
    `refresh_token_hash`,
    CASE
        WHEN `status` = 1 THEN 1
        ELSE 3
        END,
    `expires_at`,
    `created_at`,
    `revoked_at`
FROM `auth_session`;


ALTER TABLE `auth_session`
    DROP INDEX `uk_auth_session_refresh_token_hash`,
    DROP COLUMN `refresh_token_hash`;