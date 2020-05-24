package com.himadri.heartgardenreservation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationControllerTest {
    private static final String TIMEZONE = "Europe/Amsterdam";
    private RestaurantConfiguration config;
    private ReservationController reservationController;

    @BeforeEach
    void init() {
        config = new RestaurantConfiguration();
        config.setClosedDays(ImmutableList.of(2));
        config.setOpenHour(9);
        config.setOpenMinute(0);
        config.setCloseHour(16);
        config.setCloseMinute(0);
        config.setSlotInMinutes(30);
        config.setSlotsPerReservation(2);
        config.setRestaurantCapacity(5);
        config.setMaxBookAheadDays(2);
        config.setTimezone(TIMEZONE);
        reservationController = new ReservationController(config);
    }

    @Test
    void testEmptyForMonday() throws Exception {
        assertEquals(Collections.emptyList(), reservationController.getSlotsForDay("2020-05-18"));
    }

    @Test
    public void testDateParseException() throws Exception {
        assertThrows(ParseException.class, () -> reservationController.getSlotsForDay("2020-05-T12:12"));
    }

    @Test
    void testSlotsForDay() throws Exception {
        var expected = Lists.newArrayList(
            4106620800000L, 4106622600000L, 4106624400000L, 4106626200000L, 4106628000000L, 4106629800000L, 
            4106631600000L, 4106633400000L, 4106635200000L, 4106637000000L, 4106638800000L, 4106640600000L, 
            4106642400000L, 4106644200000L);
        assertEquals(expected, reservationController.getSlotsForDay("2100-02-18"));
    }

    @Test
    void testSlots() throws Exception {
        assertEquals(List.of(4106620800000L, 4106622600000L), reservationController.getSlotsToBeBooked("2100-02-18", "9:00"));
        assertEquals(List.of(4106620800000L, 4106622600000L), reservationController.getSlotsToBeBooked("2100-02-18", "9:01"));
        assertEquals(List.of(4106620800000L, 4106622600000L), reservationController.getSlotsToBeBooked("2100-02-18", "8:59"));
        assertEquals(List.of(4106620800000L, 4106622600000L), reservationController.getSlotsToBeBooked("2100-02-18", "9:14"));
        assertEquals(List.of(4106622600000L, 4106624400000L), reservationController.getSlotsToBeBooked("2100-02-18", "09:16"));
        assertEquals(List.of(4106631600000L, 4106633400000L), reservationController.getSlotsToBeBooked("2100-02-18", "12:00"));
        assertEquals(List.of(4106642400000L, 4106644200000L), reservationController.getSlotsToBeBooked("2100-02-18", "15:14"));
        assertEquals(List.of(4106644200000L), reservationController.getSlotsToBeBooked("2100-02-18", "15:16"));
        assertEquals(List.of(4106644200000L), reservationController.getSlotsToBeBooked("2100-02-18", "15:30"));
        assertEquals(List.of(4106644200000L), reservationController.getSlotsToBeBooked("2100-02-18", "15:31"));
    }

    @Test
    void getSlotDateAndTimes() throws Exception {
        config.setClosedDays(List.of());
        Map<Long, Integer> reserveMap = new HashMap<>();
        reservation(reserveMap, 9, 0 , 5);
        reservation(reserveMap, 11, 0 , 4);
        reservation(reserveMap, 13, 0 , 5);
        reservation(reserveMap, 15, 30 , 6);
        reservation(reserveMap, 16, 0 , 6);
        List<ReservationController.Slots> slotDateAndTimes = reservationController.getSlotDateAndTimes(reserveMap);
        String expectedDate = ReservationController.dateFormat.format(reserveMap.keySet().iterator().next());
        var actualSlots = slotDateAndTimes.stream()
            .filter(it -> it.getDate().equals(expectedDate))
            .findFirst()
            .orElseThrow();
        assertEquals(
            new ReservationController.Slots(
                expectedDate,
                List.of(
                    new ReservationController.SlotTimes("09:00", false),
                    new ReservationController.SlotTimes("09:30", true),
                    new ReservationController.SlotTimes("10:00", true),
                    new ReservationController.SlotTimes("10:30", true),
                    new ReservationController.SlotTimes("11:00", true),
                    new ReservationController.SlotTimes("11:30", true),
                    new ReservationController.SlotTimes("12:00", true),
                    new ReservationController.SlotTimes("12:30", false),
                    new ReservationController.SlotTimes("13:00", false),
                    new ReservationController.SlotTimes("13:30", true),
                    new ReservationController.SlotTimes("14:00", true),
                    new ReservationController.SlotTimes("14:30", true),
                    new ReservationController.SlotTimes("15:00", false),
                    new ReservationController.SlotTimes("15:30", false)
                )
            ),
            actualSlots
        );
    }

    private void reservation(Map<Long, Integer> reserveMap, int hour, int minute, int count) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(TIMEZONE));
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        reserveMap.put(calendar.getTimeInMillis(), count);
    }
}
