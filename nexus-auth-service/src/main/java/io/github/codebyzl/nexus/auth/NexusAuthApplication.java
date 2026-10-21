package io.github.codebyzl.nexus.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author: Victor_zl
 * @version: 1.0
 * @Description:
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NexusAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusAuthApplication.class, args);
        System.out.println("Nexus auth service started");
    }
}