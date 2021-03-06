package com.himadri.heartgardenreservation;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.annotations.AdminAuthorization;
import com.himadri.heartgardenreservation.annotations.FirebaseTokenParam;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.himadri.heartgardenreservation.ReservationController.dateFormat;
import static com.himadri.heartgardenreservation.ReservationController.timeFormat;

@Controller
@RequestMapping("/admin")
public class AdminController {
    public static final String X_AUTHORIZATION_FIREBASE = "X-Authorization-Firebase";
    @Autowired
    private ResourceHash resourceHash;

    @Autowired
    private GoogleCredentials googleCredentials;

    @Autowired
    private RestaurantConfiguration restaurantConfiguration;

    @PostConstruct
    public void init() throws IOException {
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
        model.addAttribute("timezone", restaurantConfiguration.getTimezone());
        return "reservations";
    }

    @RequestMapping(method = RequestMethod.GET, value = "/reservations")
    @ResponseBody
    @AdminAuthorization
    public Collection<ReservationController.Reservations> reservations(
        @RequestParam String fromDate,
        @RequestParam String toDate,
        @RequestHeader(X_AUTHORIZATION_FIREBASE) @FirebaseTokenParam String firebaseToken
    ) throws ParseException {
        long fromTimestamp = dateFormat.parse(fromDate).getTime();
        Instant twoWeeksAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        fromTimestamp = Math.max(fromTimestamp, twoWeeksAgo.toEpochMilli());
        List<Reservation> reservations = ofy().load().type(Reservation.class)
            .filter("dateTime >=", fromTimestamp)
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
                    reservation.getDateTime(),
                    new ArrayList<>(),
                    customer.getName(),
                    customer.getEmail(),
                    customer.getPhone(),
                    customer.getNbOfGuests(),
                    reservation.getReservedTables(),
                    customer.getRegistered(),
                    customer.isCancelled()
                ));

                res.getTimes().add(timeFormat.format(new Date(reservation.getDateTime())));
            }
        }
        return customerReservation.values();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/reservation")
    @ResponseBody
    @AdminAuthorization
    public String deleteReservation(
        @RequestParam String customerUUID,
        @RequestHeader(X_AUTHORIZATION_FIREBASE) @FirebaseTokenParam String firebaseToken
    ) {
        Key<Customer> customerKey = Key.create(Customer.class, customerUUID);
        List<Reservation> reservationList = ofy().load().type(Reservation.class).filter("customerKey", customerKey).list();
        ofy().delete().entities(reservationList);
        ofy().delete().key(customerKey);
        return "OK";
    }
}
