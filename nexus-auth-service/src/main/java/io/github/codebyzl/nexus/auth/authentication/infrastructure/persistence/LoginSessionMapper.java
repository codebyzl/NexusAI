package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@Mapper
public interface LoginSessionMapper extends BaseMapper<LoginSessionPo> {
}
