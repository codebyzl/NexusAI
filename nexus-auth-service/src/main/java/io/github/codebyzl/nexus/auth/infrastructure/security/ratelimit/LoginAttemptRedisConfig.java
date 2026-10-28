package io.github.codebyzl.nexus.auth.infrastructure.security.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
   public class LoginAttemptRedisConfig {

       @Bean
       public RedisScript<Long> loginAttemptFailureScript() {
           return RedisScript.of(
                   new ClassPathResource("redis/login-attempt-failure.lua"),
                   Long.class
           );
       }
   }