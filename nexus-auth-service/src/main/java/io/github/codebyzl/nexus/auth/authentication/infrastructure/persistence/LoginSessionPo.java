package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Getter
@Setter
@NoArgsConstructor
@TableName("auth_session")
public class LoginSessionPo {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private Long accountId;
    private Integer status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private Instant revokedAt;
}
