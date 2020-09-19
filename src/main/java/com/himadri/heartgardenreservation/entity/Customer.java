package com.himadri.heartgardenreservation.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Customer {
    @Id
    private String id;
    private String name;
    private String email;
    private String phone;
    private int nbOfGuests;
    private long registered;
    private boolean cancelled;
}
