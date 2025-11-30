package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProductReviewResponse {
    private Long id;
    private String authorName;
    private int rating;
    private String comment;
    private Instant createdAt;

    // true si cet avis appartient à l'utilisateur connecté
    private boolean mine;
}
