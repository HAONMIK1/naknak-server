package com.na.naknak.server.restaurant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * restaurant_images 테이블은 created_at 만 갖는다(updated_at/deleted_at 없음).
 * 따라서 BaseTimeEntity 대신 자체 감사 필드를 둔다 (review_images와 동일한 패턴).
 */
@Getter
@Entity
@Table(
        name = "restaurant_images",
        indexes = @Index(name = "idx_restaurant_images_restaurant_id", columnList = "restaurant_id")
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestaurantImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static RestaurantImage create(Restaurant restaurant, String imageUrl, int sortOrder) {
        RestaurantImage image = new RestaurantImage();
        image.restaurant = restaurant;
        image.imageUrl = imageUrl;
        image.sortOrder = sortOrder;
        return image;
    }
}
