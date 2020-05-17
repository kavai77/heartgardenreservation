package com.himadri.heartgardenreservation;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.DatastoreOptions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class ObjectifyConfig {

    @Bean
    public FilterRegistrationBean<ObjectifyFilter> objectifyFilterRegistration() {
        final FilterRegistrationBean<ObjectifyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ObjectifyFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public ServletListenerRegistrationBean<ObjectifyListener> listenerRegistrationBean() {
        ServletListenerRegistrationBean<ObjectifyListener> bean =
                new ServletListenerRegistrationBean<>();
        bean.setListener(new ObjectifyListener());
        return bean;
    }

    @WebListener
    public static class ObjectifyListener implements ServletContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            if (System.getenv("GAE_SERVICE") == null) {
                try {
                    ObjectifyService.init(new ObjectifyFactory(
                            DatastoreOptions.newBuilder()
                                    .setHost("localhost:8484")
                                    .setProjectId("my-project")
                                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream("/Users/himadri/.config/gcloud/application_default_credentials.json")))
                                    .build()
                                    .getService()
                    ));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                ObjectifyService.init(new ObjectifyFactory(
                        DatastoreOptions.getDefaultInstance().getService()
                ));
            }

            ObjectifyService.register(Customer.class);
            ObjectifyService.register(Reservation.class);
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {

        }
    }


}