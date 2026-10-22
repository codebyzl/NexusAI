package io.github.codebyzl.nexus.auth.authentication.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshToken;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenRepository;
import io.github.codebyzl.nexus.auth.authentication.domain.RefreshTokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MyBatisRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public void insert(RefreshToken refreshToken) {
        RefreshTokenPo po = RefreshTokenPersistenceConverter.toPo(refreshToken);

        int inserted = refreshTokenMapper.insert(po);
        if (inserted != 1) {
            throw new IllegalStateException("Refresh Token 创建失败");
        }
    }

    @Override
    public void update(RefreshToken refreshToken) {
        RefreshTokenPo po = RefreshTokenPersistenceConverter.toPo(refreshToken);

        int updated = refreshTokenMapper.updateById(po);
        if (updated != 1) {
            throw new IllegalStateException("Refresh Token 更新失败或不存在");
        }
    }

    @Override
    public Optional<RefreshToken> findByHash(String tokenHash) {
        requireTokenHash(tokenHash);

        RefreshTokenPo po = refreshTokenMapper.selectOne(
                new LambdaQueryWrapper<RefreshTokenPo>()
                        .eq(
                                RefreshTokenPo::getTokenHash,
                                tokenHash
                        )
        );

        return Optional.ofNullable(po)
                .map(RefreshTokenPersistenceConverter::toDomain);
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {

        requireTokenHash(tokenHash);

        RefreshTokenPo po = refreshTokenMapper.selectByHashForUpdate(tokenHash);
        return Optional.ofNullable(po)
                .map(RefreshTokenPersistenceConverter::toDomain);
    }

    @Override
    public int revokeActiveBySessionId(UUID sessionId, Instant revokedAt) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID 不能为空");
        }
        if (revokedAt == null) {
            throw new IllegalArgumentException("撤销时间不能为空");
        }

        return refreshTokenMapper.update(
                null,
                new LambdaUpdateWrapper<RefreshTokenPo>()
                        .eq(
                                RefreshTokenPo::getSessionId,
                                sessionId.toString()
                        )
                        .eq(
                                RefreshTokenPo::getStatus,
                                RefreshTokenStatus.ACTIVE.getCode()
                        )
                        .set(
                                RefreshTokenPo::getStatus,
                                RefreshTokenStatus.REVOKED.getCode()
                        )
                        .set(
                                RefreshTokenPo::getRevokedAt,
                                revokedAt
                        )
        );
    }

    private static void requireTokenHash(
            String tokenHash
    ) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException(
                    "Refresh Token Hash 不能为空"
            );
        }
    }
}
