package com.enoch.leathercraft.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;

    private String addressLine1;
    private String postalCode;
    private String city;
    private String country;
}
