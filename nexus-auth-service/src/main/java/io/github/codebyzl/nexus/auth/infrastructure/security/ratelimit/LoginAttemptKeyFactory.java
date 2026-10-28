 package io.github.codebyzl.nexus.auth.infrastructure.security.ratelimit;

   import org.springframework.stereotype.Component;

   import javax.crypto.Mac;
   import javax.crypto.SecretKey;
   import javax.crypto.spec.SecretKeySpec;
   import java.nio.charset.StandardCharsets;
   import java.util.Base64;
   import java.util.HexFormat;

   @Component
   public class LoginAttemptKeyFactory {

       private static final String HMAC_ALGORITHM = "HmacSHA256";

       private final String keyPrefix;
       private final SecretKey secretKey;

       public LoginAttemptKeyFactory(LoginAttemptProperties properties) {
           this.keyPrefix = properties.keyPrefix();

           byte[] secretBytes;

           try {
               secretBytes = Base64.getDecoder().decode(
                       properties.hmacSecret()
               );
           } catch (IllegalArgumentException exception) {
               throw new IllegalStateException(
                       "AUTH_STATE_HMAC_SECRET 必须是合法 Base64",
                       exception
               );
           }

           if (secretBytes.length < 32) {
               throw new IllegalStateException(
                       "AUTH_STATE_HMAC_SECRET 至少需要 256 bit"
               );
           }

           this.secretKey = new SecretKeySpec(
                   secretBytes,
                   HMAC_ALGORITHM
           );
       }

       public String emailFailureKey(String email) {
           return keyPrefix
                   + ":login:failure:email:"
                   + digest("email:" + email);
       }

       public String emailBlockedKey(String email) {
           return keyPrefix
                   + ":login:blocked:email:"
                   + digest("email:" + email);
       }

       public String ipFailureKey(String ip) {
           return keyPrefix
                   + ":login:failure:ip:"
                   + digest("ip:" + ip);
       }

       public String ipBlockedKey(String ip) {
           return keyPrefix
                   + ":login:blocked:ip:"
                   + digest("ip:" + ip);
       }

       private String digest(String value) {
           try {
               Mac mac = Mac.getInstance(HMAC_ALGORITHM);
               mac.init(secretKey);

               byte[] digest = mac.doFinal(
                       value.getBytes(StandardCharsets.UTF_8)
               );

               return HexFormat.of().formatHex(digest);
           } catch (Exception exception) {
               throw new IllegalStateException(
                       "Failed to generate login attempt key",
                       exception
               );
           }
       }
   }