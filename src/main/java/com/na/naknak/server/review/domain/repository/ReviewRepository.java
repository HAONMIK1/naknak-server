package com.na.naknak.server.review.domain.repository;

import com.na.naknak.server.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRestaurantIdAndDeletedAtIsNull(Long restaurantId, Pageable pageable);

    Page<Review> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<Review> findByUserIdInAndDeletedAtIsNull(Collection<Long> userIds, Pageable pageable);

    /**
     * 지역(region) 문자열이 주소에 포함된 맛집에 남긴 리뷰 수를 유저별로 집계한다
     * ("동네 맛집 마스터" 랭킹, docs/score.md 참고). region 컬럼을 따로 두지 않고
     * 기존 address 텍스트를 LIKE 매칭한다 — 행정구역 정규화는 지금 규모에서 과설계.
     */
    @Query(value = """
            SELECT r.user_id AS userId, COUNT(*) AS reviewCount
            FROM reviews r
            JOIN restaurants rest ON rest.id = r.restaurant_id
            WHERE r.deleted_at IS NULL
              AND rest.address LIKE CONCAT('%', :region, '%')
            GROUP BY r.user_id
            ORDER BY COUNT(*) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<RegionReviewCountRow> countReviewsByRegion(@Param("region") String region, @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(*) AS reviewCount
            FROM reviews r
            JOIN restaurants rest ON rest.id = r.restaurant_id
            WHERE r.deleted_at IS NULL
              AND rest.address LIKE CONCAT('%', :region, '%')
              AND r.user_id = :userId
            """, nativeQuery = true)
    long countMyReviewsByRegion(@Param("userId") Long userId, @Param("region") String region);

    @Query(value = """
            SELECT COUNT(*) FROM (
                SELECT r.user_id, COUNT(*) AS cnt
                FROM reviews r
                JOIN restaurants rest ON rest.id = r.restaurant_id
                WHERE r.deleted_at IS NULL
                  AND rest.address LIKE CONCAT('%', :region, '%')
                GROUP BY r.user_id
                HAVING COUNT(*) > :myCount
            ) ranked
            """, nativeQuery = true)
    long countUsersAheadInRegion(@Param("region") String region, @Param("myCount") long myCount);

    interface RegionReviewCountRow {
        Long getUserId();
        Long getReviewCount();
    }
}
