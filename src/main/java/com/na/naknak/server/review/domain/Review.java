package com.na.naknak.server.review.domain;

import com.na.naknak.server.common.BaseEntity;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(name = "idx_review_user_id", columnList = "user_id"),
                @Index(name = "idx_review_restaurant_id", columnList = "restaurant_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int rating;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewImage> images = new ArrayList<>();

    public static Review create(User user, Restaurant restaurant, String content, int rating) {
        Review review = new Review();
        review.user = user;
        review.restaurant = restaurant;
        review.content = content;
        review.rating = rating;
        return review;
    }

    public void addImage(String imageUrl, int sortOrder) {
        this.images.add(ReviewImage.create(this, imageUrl, sortOrder));
    }
}