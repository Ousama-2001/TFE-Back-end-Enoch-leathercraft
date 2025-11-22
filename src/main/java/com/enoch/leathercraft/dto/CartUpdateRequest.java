package com.enoch.leathercraft.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartUpdateRequest {
    private Integer quantity; // nouvelle quantité (0 => suppression)
}
