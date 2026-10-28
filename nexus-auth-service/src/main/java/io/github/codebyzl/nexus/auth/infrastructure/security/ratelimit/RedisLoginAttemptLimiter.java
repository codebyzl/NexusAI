package io.github.codebyzl.nexus.auth.infrastructure.security.ratelimit;

import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptContext;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptDecision;
import io.github.codebyzl.nexus.auth.application.security.ratelimit.LoginAttemptLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLoginAttemptLimiter implements LoginAttemptLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> loginAttemptFailureScript;
    private final LoginAttemptKeyFactory keyFactory;
    private final LoginAttemptProperties properties;

    @Override
    public LoginAttemptDecision check(LoginAttemptContext context) {
        try {
            long emailTtl = blockedTtl(
                    keyFactory.emailBlockedKey(
                            context.normalizedEmail()
                    )
            );

            long ipTtl = blockedTtl(
                    keyFactory.ipBlockedKey(
                            context.clientIp()
                    )
            );

            long retryAfterMillis =
                    Math.max(emailTtl, ipTtl);

            if (retryAfterMillis <= 0) {
                return LoginAttemptDecision.permit();
            }

            return LoginAttemptDecision.block(
                    Duration.ofMillis(retryAfterMillis)
            );
        } catch (DataAccessException exception) {
            log.error(
                    "Redis unavailable while checking login limit",
                    exception
            );

            return LoginAttemptDecision.permit();
        }
    }

    @Override
    public LoginAttemptDecision recordFailure(
            LoginAttemptContext context
    ) {
        try {
            long emailTtl = increment(
                    keyFactory.emailFailureKey(
                            context.normalizedEmail()
                    ),
                    keyFactory.emailBlockedKey(
                            context.normalizedEmail()
                    )
            );

            long ipTtl = increment(
                    keyFactory.ipFailureKey(
                            context.clientIp()
                    ),
                    keyFactory.ipBlockedKey(
                            context.clientIp()
                    )
            );

            long retryAfterMillis =
                    Math.max(emailTtl, ipTtl);

            if (retryAfterMillis <= 0) {
                return LoginAttemptDecision.permit();
            }

            return LoginAttemptDecision.block(
                    Duration.ofMillis(retryAfterMillis)
            );
        } catch (DataAccessException exception) {
            log.error(
                    "Redis unavailable while recording login failure",
                    exception
            );

            return LoginAttemptDecision.permit();
        }
    }

    @Override
    public void recordSuccess(
            LoginAttemptContext context
    ) {
        try {
            redisTemplate.delete(
                    List.of(
                            keyFactory.emailFailureKey(
                                    context.normalizedEmail()
                            ),
                            keyFactory.emailBlockedKey(
                                    context.normalizedEmail()
                            )
                    )
            );
        } catch (DataAccessException exception) {
            log.error(
                    "Redis unavailable while clearing login failures",
                    exception
            );
        }
    }

    private long increment(
            String counterKey,
            String blockedKey
    ) {
        Long result = redisTemplate.execute(
                loginAttemptFailureScript,
                List.of(counterKey, blockedKey),
                Integer.toString(
                        properties.maxFailures()
                ),
                Long.toString(
                        properties.window().toMillis()
                ),
                Long.toString(
                        properties.blockDuration().toMillis()
                )
        );

        return result == null ? 0 : result;
    }

    private long blockedTtl(String blockedKey) {
        Long ttl = redisTemplate.getExpire(
                blockedKey,
                TimeUnit.MILLISECONDS
        );

        if (ttl == null || ttl <= 0) {
            return 0;
        }

        return ttl;
    }
}