package com.himadri.heartgardenreservation;

import com.google.auth.oauth2.GoogleCredentials;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.util.Collections;

@SpringBootApplication
@PropertySource("classpath:/secrets.yml")
public class Application {
    public static final String GAE_SERVICE_ACCOUNT = "/heartgardenreservation-c9ad53c1456d.json";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        if (System.getenv("PORT") != null) {
            String port = System.getenv("PORT");
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }

        app.run(args);
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.fromStream(getClass().getResourceAsStream(Application.GAE_SERVICE_ACCOUNT));
    }

    @Bean
    public EmailValidator emailValidator() {
        return EmailValidator.getInstance();
    }
}