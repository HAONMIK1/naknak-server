package com.na.naknak.server.score.domain.repository;

import com.na.naknak.server.score.domain.UserScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserScoreRepository extends JpaRepository<UserScore, Long> {

    Optional<UserScore> findByUserId(Long userId);

    // 상위 N명은 호출부에서 Pageable(ScorePolicy.RANKING_TOP_N)로 개수를 넘긴다 —
    // 메서드 이름에 숫자를 박아두면 정책 상수와 따로 놀 수 있어 이 방식을 쓴다.
    List<UserScore> findByOrderByTotalScoreDesc(Pageable pageable);

    long countByTotalScoreGreaterThan(int totalScore);
}
