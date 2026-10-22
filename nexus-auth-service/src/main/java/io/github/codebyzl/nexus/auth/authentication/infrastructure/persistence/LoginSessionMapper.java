package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Mapper
public interface LoginSessionMapper extends BaseMapper<LoginSessionPo> {

    @Select("""
           SELECT
               id,
               account_id,
               status,
               expires_at,
               revoked_at,
               created_at,
               updated_at
           FROM auth_session
           WHERE id = #{sessionId}
           FOR UPDATE
           """)
    LoginSessionPo selectByIdForUpdate(
            @Param("sessionId") String sessionId
    );
}
