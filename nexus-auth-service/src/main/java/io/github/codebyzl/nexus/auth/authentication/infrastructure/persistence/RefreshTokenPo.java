package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@TableName("refresh_token")
public class RefreshTokenPo {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String sessionId;
    private String tokenHash;
    private Integer status;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant usedAt;
    private Instant revokedAt;
    private String replacedByTokenId;
}
