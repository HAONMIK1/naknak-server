package com.na.naknak.server.follow.domain.repository;

import com.na.naknak.server.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findByFollowerId(Long followerId);

    List<Follow> findByFollowingId(Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    @Query(value = """
            WITH RECURSIVE network(user_id, degree) AS (
                SELECT following_id, 1
                FROM follows
                WHERE follower_id = :userId

                UNION

                SELECT f.following_id, n.degree + 1
                FROM follows f
                INNER JOIN network n ON f.follower_id = n.user_id
                WHERE n.degree < :maxDegree
            )
            SELECT user_id AS userId, MIN(degree) AS degree
            FROM network
            WHERE user_id <> :userId
            GROUP BY user_id
            LIMIT :maxResults
            """, nativeQuery = true)
    List<NetworkDegreeRow> findNetworkDegrees(
            @Param("userId") Long userId,
            @Param("maxDegree") int maxDegree,
            @Param("maxResults") int maxResults
    );

    interface NetworkDegreeRow {
        Long getUserId();
        Integer getDegree();
    }
}
