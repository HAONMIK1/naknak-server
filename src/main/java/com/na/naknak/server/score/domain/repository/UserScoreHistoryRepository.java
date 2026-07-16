package com.na.naknak.server.score.domain.repository;

import com.na.naknak.server.score.domain.UserScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserScoreHistoryRepository extends JpaRepository<UserScoreHistory, Long> {
}
