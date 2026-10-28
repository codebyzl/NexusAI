 package io.github.codebyzl.nexus.auth.application.security.ratelimit;

   public interface LoginAttemptLimiter {

       LoginAttemptDecision check(
               LoginAttemptContext context
       );

       LoginAttemptDecision recordFailure(
               LoginAttemptContext context
       );

       void recordSuccess(
               LoginAttemptContext context
       );
   }