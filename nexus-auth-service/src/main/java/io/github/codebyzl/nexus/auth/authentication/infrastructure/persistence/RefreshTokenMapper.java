package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshTokenPo> {

    @Select("""
            SELECT
                id,
                session_id,
                token_hash,
                status,
                expires_at,
                created_at,
                used_at,
                revoked_at,
                replaced_by_token_id
            FROM refresh_token
            WHERE token_hash = #{tokenHash}
            FOR UPDATE
            """)
    RefreshTokenPo selectByHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}
