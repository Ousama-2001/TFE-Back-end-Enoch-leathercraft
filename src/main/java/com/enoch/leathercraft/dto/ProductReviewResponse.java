package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ProductReviewResponse {
    private Long id;
    private String authorName;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
