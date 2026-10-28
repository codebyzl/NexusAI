 package io.github.codebyzl.nexus.auth.application.security.ratelimit;

   import java.time.Duration;
   import java.util.Objects;

   public record LoginAttemptDecision(
           boolean allowed,
           Duration retryAfter
   ) {
       public LoginAttemptDecision {
           Objects.requireNonNull(retryAfter);
       }

       public static LoginAttemptDecision permit() {
           return new LoginAttemptDecision(
                   true,
                   Duration.ZERO
           );
       }

       public static LoginAttemptDecision block(
               Duration retryAfter
       ) {
           return new LoginAttemptDecision(
                   false,
                   retryAfter
           );
       }
   }