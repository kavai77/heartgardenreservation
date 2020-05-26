package com.himadri.heartgardenreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

import java.util.Collections;

@SpringBootApplication
@PropertySource("classpath:/secrets.yml")
public class Application {
    public static final String LOCAL_APPLICATION_CREDENTIALS = System.getenv("HOME") + "/.config/gcloud/application_default_credentials.json";

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        if (System.getenv("PORT") != null) {
            String port = System.getenv("PORT");
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }

        app.run(args);
    }

    @Bean
    public GoogleCloudRuntime runtime() {
        return System.getenv("GAE_SERVICE") != null ? GoogleCloudRuntime.CLOUD : GoogleCloudRuntime.LOCAL;
    }

    public enum GoogleCloudRuntime {
        CLOUD, LOCAL
    }
}