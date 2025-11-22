package com.enoch.leathercraft.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CartAddRequest {
    private Long productId;
    private Integer quantity; // quantité à ajouter (>=1)
}
