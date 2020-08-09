package com.himadri.heartgardenreservation;

import lombok.Data;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class ReservationNotAllowed {
    private final int dayOfWeek;
    private final int fromHour;
    private final int fromMinute;
    private final int toHour;
    private final int toMinute;

    private static final Pattern pattern = Pattern.compile("^(\\d)-(\\d+):(\\d+)-(\\d+):(\\d+)$");

    public static ReservationNotAllowed parse(String config) {
        Matcher matcher = pattern.matcher(config);
        if (!matcher.find()) {
            throw new IllegalArgumentException();
        }
        return new ReservationNotAllowed(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            Integer.parseInt(matcher.group(4)),
            Integer.parseInt(matcher.group(5))
        );
    }

    public boolean isNotAllowed(long time, TimeZone timezone) {
        Calendar calendar = Calendar.getInstance(timezone);
        calendar.setTimeInMillis(time);
        if (dayOfWeek != calendar.get(Calendar.DAY_OF_WEEK)) {
            return false;
        }

        Calendar fromTime = Calendar.getInstance(timezone);
        fromTime.setTimeInMillis(time);
        fromTime.set(Calendar.HOUR_OF_DAY, fromHour);
        fromTime.set(Calendar.MINUTE, fromMinute);

        Calendar toTime = Calendar.getInstance(timezone);
        toTime.setTimeInMillis(time);
        toTime.set(Calendar.HOUR_OF_DAY, toHour);
        toTime.set(Calendar.MINUTE, toMinute);

        return fromTime.compareTo(calendar) <= 0 && calendar.compareTo(toTime) <= 0;
    }
}
