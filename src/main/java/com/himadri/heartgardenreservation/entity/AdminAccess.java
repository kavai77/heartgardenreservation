package com.himadri.heartgardenreservation.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AdminAccess {
    @Id
    private String id;
    private String name;
    private String email;
    private String uid;
    private String issuer;
    private boolean emailVerified;
    private Date createDate;
}
