package io.github.codebyzl.nexus.auth.account.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.codebyzl.nexus.auth.account.domain.AccountStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@TableName("account")
public class AccountPo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String email;
    private String passwordHash;
    private AccountStatus status;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}