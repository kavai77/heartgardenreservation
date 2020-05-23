package com.himadri.heartgardenreservation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.lang.String.format;

@Controller
public class ReservationController {
    static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static final DateFormat timeFormat = new SimpleDateFormat("HH:mm");
    static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    private final Set<Integer> closed;
    private final int openHour;
    private final int closeHour;
    private final int slotInMinutes;
    private final int slotsForReservation;
    private final int maxReservationPerSlots;
    private final int maxBookAheadDays;
    private final TimeZone timezone;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ResourceHash resourceHash;

    public ReservationController(
        @Value("${restaurant.closed}") List<Integer> closed,
        @Value("${restaurant.openHour}") int openHour,
        @Value("${restaurant.closeHour}") int closeHour,
        @Value("${restaurant.slotInMinutes}") int slotInMinutes,
        @Value("${restaurant.slotsForReservation}") int slotsForReservation,
        @Value("${restaurant.maxReservationPerSlots}") int maxReservationPerSlots,
        @Value("${restaurant.maxBookAheadDays}") int maxBookAheadDays,
        @Value("${restaurant.timezone}") String timezone
    ) {
        this.closed = new HashSet<>(closed);
        this.openHour = openHour;
        this.closeHour = closeHour;
        this.slotInMinutes = slotInMinutes;
        this.slotsForReservation = slotsForReservation;
        this.maxReservationPerSlots = maxReservationPerSlots;
        this.maxBookAheadDays = maxBookAheadDays;
        this.timezone = TimeZone.getTimeZone(timezone);
        dateFormat.setTimeZone(this.timezone);
        timeFormat.setTimeZone(this.timezone);
        dateTimeFormat.setTimeZone(this.timezone);
    }


    @RequestMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("indexjshash", resourceHash.getResourceHash(ResourceHash.Resource.INDEX_JS));
        model.addAttribute("indexcsshash", resourceHash.getResourceHash(ResourceHash.Resource.INDEX_CSS));
        return "index";
    }


    @RequestMapping(method = RequestMethod.GET, value = "/_ah/warmup")
    @ResponseBody
    public String warmup() {
        return "OK";
    }

    @RequestMapping(method = RequestMethod.POST, value = "/reserve")
    public String reserve(
        @RequestParam String nameInput,
        @RequestParam String emailInput,
        @RequestParam String dateInput,
        @RequestParam String timeInput,
        @RequestParam int nbOfGuests,
        @RequestParam("g-recaptcha-response") String recatcha,
        Model model
    ) {
        try {
            final Customer customer = new Customer(UUID.randomUUID().toString(), nameInput, emailInput, nbOfGuests,
                System.currentTimeMillis());
            final Key<Customer> customerKey = ofy().save().entity(customer).now();

            List<Long> slots = getSlotsToBeBooked(dateInput, timeInput);

            boolean anyMaxSlotViolation = slots.stream()
                .map(it -> ofy().load().type(Reservation.class).filter("dateTime", it).count())
                .anyMatch(it -> it >= maxReservationPerSlots);
            if (anyMaxSlotViolation) {
                model.addAttribute("message", messageSource.getMessage("reservation.fullybooked", null, LocaleContextHolder.getLocale()));
                return "messagePage";
            }

            slots.forEach(it -> ofy().save().entity(new Reservation(UUID.randomUUID().toString(), it, customerKey)));

            model.addAttribute("message", messageSource.getMessage("reservation.success", null, LocaleContextHolder.getLocale()));
            return "messagePage";
        } catch (Exception e) {
            model.addAttribute("message", messageSource.getMessage("reservation.generalerror", null, LocaleContextHolder.getLocale()));
            return "messagePage";
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/slots")
    @ResponseBody
    public List<Slots> slots() throws ParseException {
        List<Reservation> reservations = ofy().load().type(Reservation.class).filter("dateTime >=", System.currentTimeMillis()).list();
        Map<Long, Integer> reservationCount = new HashMap<>();
        reservations.forEach(it -> reservationCount.compute(it.getDateTime(), (k, v) -> (v == null ? 1 : v + 1)));
        return getSlotDateAndTimes(reservationCount);
    }


    @VisibleForTesting
    List<Slots> getSlotDateAndTimes(Map<Long, Integer> reservationCount) throws ParseException {
        Calendar date = Calendar.getInstance(timezone);
        var slots = new ArrayList<Slots>();
        for (int i = 0; i <= maxBookAheadDays; i++) {
            String day = dateFormat.format(date.getTime());
            List<Long> slotsForDay = getSlotsForDay(day);
            if (!slotsForDay.isEmpty()) {
                List<SlotTimes> slotTimes = new ArrayList<>();
                slots.add(new Slots(day, slotTimes));
                for (Long slot : slotsForDay) {
                    boolean free = reservationCount.getOrDefault(slot, 0) < maxReservationPerSlots;
                    SlotTimes time = new SlotTimes(timeFormat.format(new Date(slot)), free);
                    if (!free) {
                        for (int j = slotTimes.size() - 1; j >= Math.max(slotTimes.size() - slotsForReservation + 1, 0); j--) {
                            slotTimes.get(j).setFree(false);
                        }
                    }
                    slotTimes.add(time);
                }
            }
            date.add(Calendar.DATE, 1);
        }
        return slots;
    }


    @VisibleForTesting
    List<Long> getSlotsToBeBooked(String dateInput, String timeInput) throws ParseException {
        Calendar date = Calendar.getInstance(timezone);
        date.setTime(dateTimeFormat.parse(format("%sT%s", dateInput, timeInput)));
        var slotsForDay = getSlotsForDay(dateInput);
        final long timeInMillis = date.getTimeInMillis();
        int index = Collections.binarySearch(slotsForDay, timeInMillis);
        if (index < 0) {
            var candidate = -index - 1;
            if (candidate == 0) {
                index = 0;
            } else if (candidate == slotsForDay.size()) {
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
        for (int i = index + 1; i < Math.min(index + slotsForReservation, slotsForDay.size()); i++) {
            list.add(slotsForDay.get(i));
        }
        return list;
    }

    @VisibleForTesting
    List<Long> getSlotsForDay(String dateInput) throws ParseException {
        Calendar date = Calendar.getInstance(timezone);
        date.setTime(dateFormat.parse(dateInput));
        if (closed.contains(date.get(Calendar.DAY_OF_WEEK))) {
            return Collections.emptyList();
        }

        date.set(Calendar.HOUR_OF_DAY, openHour);
        date.set(Calendar.MINUTE, 0);
        var list = new ArrayList<Long>();
        while (date.get(Calendar.HOUR_OF_DAY) < closeHour) {
            if (date.getTimeInMillis() >= System.currentTimeMillis()) {
                list.add(date.getTimeInMillis());
            }
            date.add(Calendar.MINUTE, slotInMinutes);
        }
        return list;
    }

    @Data
    public static class Slots {
        private final String date;
        private final List<SlotTimes> slotTimes;
    }

    @Data
    @AllArgsConstructor
    public static class SlotTimes {
        private final String time;
        private boolean free;
    }

    @Data
    @AllArgsConstructor
    public static class Reservations {
        private final String customerUUID;
        private final String date;
        private final List<String> times;
        private final String name;
        private final String email;
        private final int nbOfGuests;
        private final String registered;
    }
}