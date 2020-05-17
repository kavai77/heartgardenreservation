package com.himadri.heartgardenreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        String port;
        if (System.getenv("PORT") != null) {
            port = System.getenv("PORT");
        } else {
            port = "8080";
        }
        app.setDefaultProperties(Collections.singletonMap("server.port", port));
        app.run(args);
    }
}