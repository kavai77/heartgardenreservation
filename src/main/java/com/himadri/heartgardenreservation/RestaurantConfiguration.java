package com.himadri.heartgardenreservation;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Data
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "restaurant")
public class RestaurantConfiguration {
    @Value("${restaurant.closedDays}")
    private List<Integer> closedDays;

    @Value("${restaurant.openHour}")
    private int openHour;

    @Value("${restaurant.openMinute}")
    private int openMinute;

    @Value("${restaurant.closeHour}")
    private int closeHour;

    @Value("${restaurant.closeMinute}")
    private int closeMinute;

    @Value("${restaurant.slotInMinutes}")
    private int slotInMinutes;

    @Value("${restaurant.slotsPerReservation}")
    private int slotsPerReservation;

    @Value("${restaurant.restaurantCapacity}")
    private int restaurantCapacity;

    @Value("${restaurant.maxBookAheadDays}")
    private int maxBookAheadDays;

    @Value("${restaurant.timezone}")
    private String timezone;

    @Value("${restaurant.fromemail}")
    private String fromEmail;

    @Value("${restaurant.oneHouseHoldLimitInForm}")
    private int oneHouseHoldLimitInForm;

    @Value("${restaurant.goLive}")
    private String goLive;

    private Map<Integer, Integer> guestTableNbMap;
}
