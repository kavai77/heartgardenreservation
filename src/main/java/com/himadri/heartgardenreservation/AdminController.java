package com.himadri.heartgardenreservation;

import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.ParseException;
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
import static com.himadri.heartgardenreservation.ReservationController.dateTimeFormat;
import static com.himadri.heartgardenreservation.ReservationController.timeFormat;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private ResourceHash resourceHash;

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
        @RequestParam String toDate
    ) throws ParseException {
        List<Reservation> reservations = ofy().load().type(Reservation.class)
            .filter("dateTime >=", dateFormat.parse(fromDate).getTime())
            .filter("dateTime <",dateFormat.parse(toDate).getTime() + 1000 * 60 * 60 * 24)
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
                    dateTimeFormat.format(new Date(customer.getRegistered()))
                ));

                res.getTimes().add(timeFormat.format(new Date(reservation.getDateTime())));
            }
        }
        return customerReservation.values();
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/reservation")
    @ResponseBody
    public String deleteReservation(@RequestParam String customerUUID) {
        Key<Customer> customerKey = Key.create(Customer.class, customerUUID);
        List<Reservation> reservationList = ofy().load().type(Reservation.class).filter("customerKey", customerKey).list();
        ofy().delete().entities(reservationList);
        ofy().delete().key(customerKey);
        return "OK";
    }

}
