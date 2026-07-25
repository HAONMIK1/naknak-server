package com.na.naknak.server.restaurant.domain;

import com.na.naknak.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "restaurants",
        indexes = {
                @Index(name = "idx_restaurant_naver_place_id", columnList = "naver_place_id"),
                @Index(name = "idx_restaurant_name", columnList = "name"),
                @Index(name = "idx_restaurant_category", columnList = "category")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "naver_place_id", unique = true)
    private String naverPlaceId;

    @Column(name = "naver_place_url")
    private String naverPlaceUrl;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String address;

    private Double latitude;

    private Double longitude;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RestaurantImage> images = new ArrayList<>();

    public static Restaurant create(
            String naverPlaceId,
            String naverPlaceUrl,
            String name,
            String category,
            String address,
            Double latitude,
            Double longitude
    ) {
        Restaurant restaurant = new Restaurant();
        restaurant.naverPlaceId = naverPlaceId;
        restaurant.naverPlaceUrl = naverPlaceUrl;
        restaurant.name = name;
        restaurant.category = category;
        restaurant.address = address;
        restaurant.latitude = latitude;
        restaurant.longitude = longitude;
        return restaurant;
    }

    public void addImage(String imageUrl, int sortOrder) {
        this.images.add(RestaurantImage.create(this, imageUrl, sortOrder));
    }
}