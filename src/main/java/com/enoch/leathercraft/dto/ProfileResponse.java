package com.enoch.leathercraft.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    // Adresse de livraison
    private String addressLine1;
    private String postalCode;
    private String city;
    private String country;
}
