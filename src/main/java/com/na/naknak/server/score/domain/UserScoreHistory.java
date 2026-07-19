package com.na.naknak.server.score.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * user_score_history 테이블은 created_at 만 갖는다(updated_at/deleted_at 없음).
 * 따라서 BaseTimeEntity 대신 자체 감사 필드를 둔다 (follows, review_images와 동일한 패턴).
 * 감사·문의 대응용 원장이라 삭제하지 않는다.
 */
@Getter
@Entity
@Table(
        name = "user_score_history",
        indexes = @Index(name = "idx_score_history_user_id", columnList = "user_id")
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScoreHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScoreTarget target;

    @Column(nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScoreReason reason;

    @Column(name = "review_id")
    private Long reviewId;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static UserScoreHistory create(Long userId, ScoreTarget target, int delta, ScoreReason reason, Long reviewId) {
        UserScoreHistory history = new UserScoreHistory();
        history.userId = userId;
        history.target = target;
        history.delta = delta;
        history.reason = reason;
        history.reviewId = reviewId;
        return history;
    }
}
