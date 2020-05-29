package com.himadri.heartgardenreservation;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.entity.AdminAccess;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.himadri.heartgardenreservation.ReservationController.dateFormat;
import static com.himadri.heartgardenreservation.ReservationController.dateTimeFormat;
import static com.himadri.heartgardenreservation.ReservationController.timeFormat;

@Controller
@RequestMapping("/admin")
public class AdminController {
    public static final String X_AUTHORIZATION_FIREBASE = "X-Authorization-Firebase";
    @Autowired
    private ResourceHash resourceHash;

    @Autowired
    private Application.GoogleCloudRuntime runtime;

    @Value("${tokenIssuer}")
    private String tokenIssuer;

    @Value("${accountsWhitelistedForAdmin}")
    private List<String> accountsWhitelistedForAdmin;

    @PostConstruct
    public void init() throws IOException {
        GoogleCredentials googleCredentials;
        if (runtime == Application.GoogleCloudRuntime.LOCAL) {
            try {
                googleCredentials = GoogleCredentials.fromStream(new FileInputStream(Application.LOCAL_APPLICATION_CREDENTIALS));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            googleCredentials = GoogleCredentials.getApplicationDefault();
        }
        FirebaseOptions options = new FirebaseOptions.Builder()
            .setCredentials(googleCredentials)
            .setProjectId("heartgardenreservation")
            .setStorageBucket("heartgardenreservation.appspot.com")
            .setDatabaseUrl("https://heartgardenreservation.firebaseio.com/")
            .build();
        FirebaseApp.initializeApp(options);
    }

    @RequestMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("reservationsjshash", resourceHash.getResourceHash(ResourceHash.Resource.RESERVATION_JS));
        model.addAttribute("reservationscsshash", resourceHash.getResourceHash(ResourceHash.Resource.RESERVATION_CSS));
        return "reservations";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/reservations")
    @ResponseBody
    public Collection<ReservationController.Reservations> reservations(
        @RequestParam String fromDate,
        @RequestParam String toDate,
        @RequestHeader(X_AUTHORIZATION_FIREBASE) String firebaseToken
    ) throws ParseException, FirebaseAuthException {
        checkAuthorization(firebaseToken);
        List<Reservation> reservations = ofy().load().type(Reservation.class)
            .filter("dateTime >=", dateFormat.parse(fromDate).getTime())
            .filter("dateTime <",dateFormat.parse(toDate).getTime())
            .list();
        Set<String> keySet = reservations.stream()
            .map(it -> it.getCustomerKey().getName())
            .collect(Collectors.toSet());
        Map<String, Customer> customers = ofy().load().type(Customer.class).ids(keySet);
        var customerReservation = new LinkedHashMap<String, ReservationController.Reservations>();
        for (var reservation: reservations) {
            var customer = customers.get(reservation.getCustomerKey().getName());
            if (customer != null) {
                ReservationController.Reservations res = customerReservation.computeIfAbsent(customer.getId(), k -> new ReservationController.Reservations(
                    customer.getId(),
                    dateFormat.format(new Date(reservation.getDateTime())),
                    new ArrayList<>(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getNbOfGuests(),
                    reservation.getReservedTables(),
                    dateTimeFormat.format(new Date(customer.getRegistered()))
                ));

                res.getTimes().add(timeFormat.format(new Date(reservation.getDateTime())));
            }
        }
        return customerReservation.values();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/reservation")
    @ResponseBody
    public String deleteReservation(
        @RequestParam String customerUUID,
        @RequestHeader(X_AUTHORIZATION_FIREBASE) String firebaseToken
    ) throws FirebaseAuthException {
        checkAuthorization(firebaseToken);
        Key<Customer> customerKey = Key.create(Customer.class, customerUUID);
        List<Reservation> reservationList = ofy().load().type(Reservation.class).filter("customerKey", customerKey).list();
        ofy().delete().entities(reservationList);
        ofy().delete().key(customerKey);
        return "OK";
    }


    private void checkAuthorization(String firebaseToken) throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
        if (!StringUtils.equals(decodedToken.getIssuer(), tokenIssuer) ||
            !decodedToken.isEmailVerified() ||
            !accountsWhitelistedForAdmin.contains(decodedToken.getEmail())) {
            ofy().save().entity(new AdminAccess(
                UUID.randomUUID().toString(),
                decodedToken.getName(),
                decodedToken.getEmail(),
                decodedToken.getUid(),
                decodedToken.getIssuer(),
                decodedToken.isEmailVerified(),
                new Date()
            ));
            throw new AdminAuthorizationException();
        }
    }

    @ResponseStatus(value= HttpStatus.UNAUTHORIZED)
    public static class AdminAuthorizationException extends RuntimeException{}
}
