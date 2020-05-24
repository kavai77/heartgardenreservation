package com.himadri.heartgardenreservation;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

public class SendGridTest {
    @Test
    @Disabled
    public void testEmail() throws Exception{
        YamlPropertySourceLoader jsonParser = new YamlPropertySourceLoader();
        PropertySource<?> secrets = jsonParser.load("secrets", new ClassPathResource("/secrets.yml")).get(0);
        PropertySource<?> config = jsonParser.load("config", new ClassPathResource("/application.yml")).get(0);
        Properties messages = new Properties();
        messages.load(getClass().getResourceAsStream("/messages.properties"));

        Email from = new Email(config.getProperty("restaurant.fromemail").toString());
        Email to = new Email(secrets.getProperty("adminemail").toString());
        Mail mail = new Mail();
        mail.setFrom(from);
        Personalization personalization = new Personalization();
        personalization.addTo(to);
        personalization.addDynamicTemplateData("name", "Himadri");
        personalization.addDynamicTemplateData("date", "Monday, June 8th, 2020");
        personalization.addDynamicTemplateData("time", "12:30");
        personalization.addDynamicTemplateData("cancellationlink", "https://heartgardenreservation.appspot.com/cancel");
        mail.addPersonalization(personalization);
        mail.setTemplateId(messages.getProperty("confirmationemail"));

        SendGrid sg = new SendGrid(secrets.getProperty("sendgridapikey").toString());
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        Response response = sg.api(request);
        System.out.println(response.getStatusCode());
        System.out.println(response.getBody());
        System.out.println(response.getHeaders());
    }
}
