package com.na.naknak.server.follow.domain.repository;

import com.na.naknak.server.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findByFollowerId(Long followerId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);
}
