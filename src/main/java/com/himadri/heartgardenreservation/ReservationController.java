package com.himadri.heartgardenreservation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.himadri.heartgardenreservation.entity.Customer;
import com.himadri.heartgardenreservation.entity.Reservation;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.lang.String.format;

@Controller
public class ReservationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationController.class);

    static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    static final DateFormat timeFormat = new SimpleDateFormat("HH:mm");
    static final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

    private final RestaurantConfiguration restaurantConfiguration;
    private final TimeZone timezone;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ResourceHash resourceHash;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Value("${recaptchasecret}")
    private String recaptchaSecret;

    @Value("${sendgridapikey}")
    private String sendgridApiKey;

    public ReservationController(RestaurantConfiguration restaurantConfiguration) {
        this.restaurantConfiguration = restaurantConfiguration;

        this.timezone = TimeZone.getTimeZone(restaurantConfiguration.getTimezone());
        dateFormat.setTimeZone(this.timezone);
        timeFormat.setTimeZone(this.timezone);
        dateTimeFormat.setTimeZone(this.timezone);
    }


    @RequestMapping(value = "/")
    public String index(Model model) {
        model.addAttribute("indexjshash", resourceHash.getResourceHash(ResourceHash.Resource.INDEX_JS));
        model.addAttribute("indexcsshash", resourceHash.getResourceHash(ResourceHash.Resource.INDEX_CSS));
        model.addAttribute("maxGuestInForm", restaurantConfiguration.getMaxGuestInForm());
        model.addAttribute("oneHouseHoldLimitInForm", restaurantConfiguration.getOneHouseHoldLimitInForm());
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
        @RequestParam("g-recaptcha-response") String recaptchaResponse,
        HttpServletRequest request,
        Model model
    ) {
        try {
            verifyRecatcha(recaptchaResponse, request.getRemoteAddr());
            final Customer customer = new Customer(UUID.randomUUID().toString(), nameInput, emailInput, nbOfGuests,
                System.currentTimeMillis());
            final Key<Customer> customerKey = ofy().save().entity(customer).now();

            List<Long> slots = getSlotsToBeBooked(dateInput, timeInput);

            boolean anyMaxSlotViolation = slots.stream()
                .map(it -> ofy().load().type(Reservation.class).filter("dateTime", it).count())
                .anyMatch(it -> it >= restaurantConfiguration.getRestaurantCapacity());
            if (anyMaxSlotViolation) {
                model.addAttribute("title", getMessage("reservation.fullybooked.title"));
                model.addAttribute("body", getMessage("reservation.fullybooked.body"));
                return "message";
            }

            slots.forEach(it -> ofy().save().entity(new Reservation(UUID.randomUUID().toString(), it, customerKey)));

            try {
                sendConfirmationEmail(customer, slots.get(0));
            } catch (SendGridException e) {
                LOGGER.error("Sendgrid exception", e);
            }

            model.addAttribute("title", getMessage("reservation.success.title"));
            model.addAttribute("body", getMessage("reservation.success.body"));
            return "message";
        } catch (RecaptchaException e) {
            LOGGER.info("Recaptcha verification failed {}", e.getRecaptchaResponse());
            model.addAttribute("title", getMessage("reservation.generalerror.title"));
            model.addAttribute("body", getMessage("reservation.recatchaerror"));
            return "message";
        } catch (Exception e) {
            LOGGER.error("Error", e);
            model.addAttribute("title", getMessage("reservation.generalerror.title"));
            model.addAttribute("body", getMessage("reservation.generalerror.body"));
            return "message";
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
        for (int i = 0; i <= restaurantConfiguration.getMaxBookAheadDays(); i++) {
            String day = dateFormat.format(date.getTime());
            List<Long> slotsForDay = getSlotsForDay(day);
            if (!slotsForDay.isEmpty()) {
                List<SlotTimes> slotTimes = new ArrayList<>();
                slots.add(new Slots(day, slotTimes));
                for (Long slot : slotsForDay) {
                    boolean free = reservationCount.getOrDefault(slot, 0) < restaurantConfiguration.getRestaurantCapacity();
                    SlotTimes time = new SlotTimes(timeFormat.format(new Date(slot)), free);
                    if (!free) {
                        for (int j = slotTimes.size() - 1; j >= Math.max(slotTimes.size() - restaurantConfiguration.getSlotsPerReservation() + 1, 0); j--) {
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
        for (int i = index + 1; i < Math.min(index + restaurantConfiguration.getSlotsPerReservation(), slotsForDay.size()); i++) {
            list.add(slotsForDay.get(i));
        }
        return list;
    }

    @VisibleForTesting
    List<Long> getSlotsForDay(String dateInput) throws ParseException {
        Calendar date = Calendar.getInstance(timezone);
        date.setTime(dateFormat.parse(dateInput));
        if (restaurantConfiguration.getClosedDays().contains(date.get(Calendar.DAY_OF_WEEK))) {
            return Collections.emptyList();
        }

        Calendar closeTime = Calendar.getInstance(timezone);
        closeTime.setTime(dateFormat.parse(dateInput));
        closeTime.set(Calendar.HOUR_OF_DAY, restaurantConfiguration.getCloseHour());
        closeTime.set(Calendar.MINUTE, restaurantConfiguration.getCloseMinute());

        date.set(Calendar.HOUR_OF_DAY, restaurantConfiguration.getOpenHour());
        date.set(Calendar.MINUTE, restaurantConfiguration.getOpenMinute());
        var list = new ArrayList<Long>();
        while (date.compareTo(closeTime) < 0) {
            if (date.getTimeInMillis() >= System.currentTimeMillis()) {
                list.add(date.getTimeInMillis());
            }
            date.add(Calendar.MINUTE, restaurantConfiguration.getSlotInMinutes());
        }
        return list;
    }

    private void verifyRecatcha(String recaptchaResponse, String remoteAddr ) throws RecaptchaException {
        RestTemplate restTemplate = restTemplateBuilder.build();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl("https://www.google.com/recaptcha/api/siteverify")
            .queryParam("secret", recaptchaSecret)
            .queryParam("response", recaptchaResponse)
            .queryParam("remoteip", remoteAddr);

        ResponseEntity<RecaptchaResponse> entity = restTemplate.postForEntity(
            uriBuilder.toUriString(), null,
            RecaptchaResponse.class);
        if (entity.getStatusCode()== HttpStatus.OK) {
            RecaptchaResponse response = entity.getBody();
            if (!response.isSuccess()) {
                throw new RecaptchaException(response);
            }
        }
    }

    private void sendConfirmationEmail(Customer customer, Long firstSlot) throws SendGridException {
        try {
            DateFormat localDateFormat = SimpleDateFormat.getDateInstance(DateFormat.FULL, LocaleContextHolder.getLocale());
            Email from = new Email(restaurantConfiguration.getFromEmail());
            Email to = new Email(customer.getEmail());
            Mail mail = new Mail();
            mail.setFrom(from);
            Personalization personalization = new Personalization();
            personalization.addTo(to);
            personalization.addDynamicTemplateData("name", customer.getName());
            personalization.addDynamicTemplateData("guests", customer.getNbOfGuests() == 1 ? getMessage("oneguest") : getMessage("moreguests", customer.getNbOfGuests()));
            personalization.addDynamicTemplateData("date", localDateFormat.format(new Date(firstSlot)));
            personalization.addDynamicTemplateData("time", timeFormat.format(new Date(firstSlot)));
            UriComponentsBuilder cancellationLinkBuilder = UriComponentsBuilder
                .fromHttpUrl("https://heartgardenreservation.appspot.com/cancel")
                .queryParam("customerUUID", customer.getId());
            personalization.addDynamicTemplateData("cancellationlink", cancellationLinkBuilder.build().toUriString());
            mail.addPersonalization(personalization);
            mail.setTemplateId(getMessage("confirmationemail"));

            SendGrid sg = new SendGrid(sendgridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            if (!HttpStatus.valueOf(response.getStatusCode()).is2xxSuccessful()) {
                throw new SendGridException(response.getStatusCode(), response.getBody());
            }
        } catch (IOException | RuntimeException e) {
            throw new SendGridException(e);
        }
    }

    private String getMessage(String key, Object... params) {
        return messageSource.getMessage(key, params, LocaleContextHolder.getLocale());
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

    @Data
    private static class RecaptchaResponse {
        private boolean success;
        @JsonAlias("challenge_ts")
        private String challengeTs;
        private String hostname;
        @JsonAlias("error-codes")
        private List<String> errorCodes;
    }

    @Getter
    @RequiredArgsConstructor
    private static class RecaptchaException extends Exception {
        private final RecaptchaResponse recaptchaResponse;
    }

    @Getter
    @AllArgsConstructor
    private static class SendGridException extends Exception {
        private int httpStatusCode;
        private String httpBody;

        public SendGridException(Throwable cause) {
            super(cause);
        }
    }
}
