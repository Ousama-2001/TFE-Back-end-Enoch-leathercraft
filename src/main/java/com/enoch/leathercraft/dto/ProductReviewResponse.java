// src/main/java/com/enoch/leathercraft/dto/ProductReviewResponse.java
package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.entities.ReviewStatus; // Assure-toi d'importer l'enum
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

    private boolean mine; // Est-ce mon avis ?

    // ✅ AJOUT IMPORTANT
    private ReviewStatus status;
}