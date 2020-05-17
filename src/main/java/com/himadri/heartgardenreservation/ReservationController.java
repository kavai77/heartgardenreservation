package com.himadri.heartgardenreservation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.lang.String.format;

@Controller
public class ReservationController {
    private static final Log LOGGER = LogFactory.getLog(ReservationController.class);
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-z");
    private static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm-z");
    private static final String UTC = "UTC";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(UTC);

    @RequestMapping(method = RequestMethod.POST, value = "/reserve")
    public String reserve(
            @RequestParam String nameInput,
            @RequestParam String emailInput,
            @RequestParam String dateInput,
            @RequestParam String timeInput,
            @RequestParam("g-recaptcha-response") String recatcha
    ) {
//        return ofy().transactNew( () -> {
            try {
                final Customer customer = new Customer(UUID.randomUUID().toString(), nameInput, emailInput, System.currentTimeMillis());
                final Key<Customer> customerKey = ofy().save().entity(customer).now();

                dateInput = dateInput.substring(0, 10);
                List<Long> slots = getSlots(dateInput, timeInput);
                for (var slot : slots) {
                    Reservation reservation = ofy().load().type(Reservation.class).id(slot).now();
                    if (reservation == null) {
                        reservation = new Reservation();
                        reservation.setDateTime(slot);
                    }
                    reservation.getCustomers().add(customerKey);
                    ofy().save().entity(reservation);
                }
                return "confirmation";
            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }
//        });
    }

    @VisibleForTesting
    List<Long> getSlots(String dateInput, String timeInput)  throws ParseException {
        Calendar date = Calendar.getInstance(UTC_TIME_ZONE);
        date.setTime(dateTimeFormat.parse(format("%s-%s-%s", dateInput, timeInput, UTC)));
        var slotsForDay = getSlotsForDay(dateInput);
        final long timeInMillis = date.getTimeInMillis();
        int index = Collections.binarySearch(slotsForDay, timeInMillis);
        if (index < 0) {
            var candidate = -index - 1;
            if (candidate == 0) {
                index = 0;
            } else if(candidate == slotsForDay.size()) {
                index = slotsForDay.size() - 1;
            } else {
                if (timeInMillis - slotsForDay.get(candidate - 1) < slotsForDay.get(candidate) - timeInMillis) {
                    index = candidate - 1;
                } else {
                    index = candidate;
                }
            }
        }
        var list = Lists.newArrayList(slotsForDay.get(index));
        if (index < slotsForDay.size() - 1) {
            list.add(slotsForDay.get(index + 1));
        }
        return list;
    }

    @VisibleForTesting
    List<Long> getSlotsForDay(String dateInput) throws ParseException {
        Calendar date = Calendar.getInstance(UTC_TIME_ZONE);
        date.setTime(dateFormat.parse(format("%s-%s",dateInput, UTC)));
        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            return Collections.emptyList();
        }

        date.set(Calendar.HOUR_OF_DAY, 9);
        date.set(Calendar.MINUTE, 0);
        var list = new ArrayList<Long>();
        while (date.get(Calendar.HOUR_OF_DAY) < 16) {
            list.add(date.getTimeInMillis());
            date.add(Calendar.MINUTE, 30);
        }
        return list;
    }

}
