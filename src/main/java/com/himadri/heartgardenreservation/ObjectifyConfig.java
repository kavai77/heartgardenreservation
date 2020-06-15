package com.himadri.heartgardenreservation;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.DatastoreOptions;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.ObjectifyService;
import com.himadri.heartgardenreservation.entity.AdminAccess;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

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
    public ServletListenerRegistrationBean<ObjectifyListener> listenerRegistrationBean(GoogleCredentials googleCredentials) {
        ServletListenerRegistrationBean<ObjectifyListener> bean =
                new ServletListenerRegistrationBean<>();
        bean.setListener(new ObjectifyListener(googleCredentials));
        return bean;
    }

    @WebListener
    public static class ObjectifyListener implements ServletContextListener {
        private final GoogleCredentials googleCredentials;

        public ObjectifyListener(GoogleCredentials googleCredentials) {
            this.googleCredentials = googleCredentials;
        }

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            ObjectifyService.init(new ObjectifyFactory(
                DatastoreOptions.newBuilder()
                    .setCredentials(googleCredentials)
                    .build()
                    .getService()
            ));

            ObjectifyService.register(Customer.class);
            ObjectifyService.register(Reservation.class);
            ObjectifyService.register(AdminAccess.class);
        }
    }
}