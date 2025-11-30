// src/main/java/com/enoch/leathercraft/dto/AdminReviewResponse.java
package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.entities.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminReviewResponse {

    private Long id;

    private Long productId;
    private String productName;

    private Long userId;
    private String userEmail;
    private String authorName;

    private int rating;
    private String comment;

    private ReviewStatus status;
    private Instant createdAt;
    private Instant deletedAt;
}
