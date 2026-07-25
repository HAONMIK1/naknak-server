package com.na.naknak.server.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * review_images 테이블은 created_at 만 갖는다(updated_at/deleted_at 없음).
 * 따라서 BaseTimeEntity 대신 자체 감사 필드를 둔다.
 */
@Getter
@Entity
@Table(
        name = "review_images",
        indexes = @Index(name = "idx_review_images_review_id", columnList = "review_id")
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static ReviewImage create(Review review, String imageUrl, int sortOrder) {
        ReviewImage reviewImage = new ReviewImage();
        reviewImage.review = review;
        reviewImage.imageUrl = imageUrl;
        reviewImage.sortOrder = sortOrder;
        return reviewImage;
    }
}
