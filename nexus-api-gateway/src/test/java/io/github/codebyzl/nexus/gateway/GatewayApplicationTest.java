   package io.github.codebyzl.nexus.gateway;

   import org.junit.jupiter.api.Test;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.boot.test.context.SpringBootTest;
   import org.springframework.test.web.reactive.server.WebTestClient;

   @SpringBootTest(
           webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
   )
   class GatewayApplicationTest {

       @Autowired
       private WebTestClient webTestClient;

       @Test
       void shouldExposeHealthEndpoint() {
           webTestClient.get()
                   .uri("/actuator/health")
                   .exchange()
                   .expectStatus().isOk()
                   .expectBody()
                   .jsonPath("$.status").isEqualTo("UP");
       }

       @Test
       void shouldRejectAnonymousRequestOutsideAllowedPaths() {
           webTestClient.get()
                   .uri("/api/not-configured")
                   .exchange()
                   .expectStatus().isUnauthorized()
                   .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                   .expectBody()
                   .jsonPath("$.code").isEqualTo("INVALID_ACCESS_TOKEN");
       }
   }