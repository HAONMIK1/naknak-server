package com.na.naknak.server.restaurant.domain.repository;

import com.na.naknak.server.restaurant.domain.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByNameAndAddress(String name, String address);

    Page<Restaurant> findByNameContaining(String keyword, Pageable pageable);
}