package com.himadri.heartgardenreservation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationNotAllowedTest {
    @Test
    void testParsing() {
        assertEquals(new ReservationNotAllowed(7, 10, 30, 14, 0), ReservationNotAllowed.parse("7-10:30-14:00"));
        assertThrows(IllegalArgumentException.class, () -> ReservationNotAllowed.parse("7-10:30-14:00 "));
    }
}