package com.himadri.heartgardenreservation;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReservationControllerTest {
    private ReservationController reservationController;

    @BeforeEach
    void init() {
        reservationController = new ReservationController();
    }

    @Test
    void testEmptyForMonday() throws Exception {
        assertEquals(Collections.emptyList(), reservationController.getSlotsForDay("2020-05-18"));
    }

    @Test
    public void testDateParseException() throws Exception {
        assertThrows(ParseException.class, () -> reservationController.getSlotsForDay("2020-05-28T12:12"));
    }

    @Test
    void testSlotsForDay() throws Exception {
        var expected = Lists.newArrayList(
                1589706000000L, 1589707800000L, 1589709600000L, 1589711400000L, 1589713200000L,
                1589715000000L, 1589716800000L, 1589718600000L, 1589720400000L, 1589722200000L, 1589724000000L,
                1589725800000L, 1589727600000L, 1589729400000L);
        assertEquals(expected, reservationController.getSlotsForDay("2020-05-17"));
    }

    @Test
    void testSlots() throws Exception {
        assertEquals(List.of(1589706000000L, 1589707800000L), reservationController.getSlots("2020-05-17", "9:00"));
        assertEquals(List.of(1589706000000L, 1589707800000L), reservationController.getSlots("2020-05-17", "9:01"));
        assertEquals(List.of(1589706000000L, 1589707800000L), reservationController.getSlots("2020-05-17", "8:59"));
        assertEquals(List.of(1589706000000L, 1589707800000L), reservationController.getSlots("2020-05-17", "9:14"));
        assertEquals(List.of(1589707800000L, 1589709600000L), reservationController.getSlots("2020-05-17", "09:16"));
        assertEquals(List.of(1589716800000L, 1589718600000L), reservationController.getSlots("2020-05-17", "12:00"));
        assertEquals(List.of(1589727600000L, 1589729400000L), reservationController.getSlots("2020-05-17", "15:14"));
        assertEquals(List.of(1589729400000L), reservationController.getSlots("2020-05-17", "15:16"));
        assertEquals(List.of(1589729400000L), reservationController.getSlots("2020-05-17", "15:30"));
        assertEquals(List.of(1589729400000L), reservationController.getSlots("2020-05-17", "15:31"));
    }
}
