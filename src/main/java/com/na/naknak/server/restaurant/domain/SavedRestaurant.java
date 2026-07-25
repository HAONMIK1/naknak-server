package com.na.naknak.server.restaurant.domain;

import com.na.naknak.server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * saved_restaurants 테이블은 created_at 만 갖는다(updated_at/deleted_at 없음).
 * 따라서 BaseTimeEntity 대신 자체 감사 필드를 둔다 (follows, review_images와 동일한 패턴).
 */
@Getter
@Entity
@Table(
        name = "saved_restaurants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_saved_restaurant_user_restaurant",
                columnNames = {"user_id", "restaurant_id"}
        )
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedRestaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static SavedRestaurant create(User user, Restaurant restaurant) {
        SavedRestaurant saved = new SavedRestaurant();
        saved.user = user;
        saved.restaurant = restaurant;
        return saved;
    }
}
