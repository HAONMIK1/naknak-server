package com.na.naknak.server.score.application;

import com.na.naknak.server.score.domain.ScoreReason;

/**
 * "이 유저에게 이 사유로 점수를 적립해야 한다"는 사실만 담은 이벤트.
 * SQS 큐에 JSON으로 실려서 producer(ReviewService/UserService)와
 * consumer(ScoreEarnConsumer)를 분리한다 (docs/score.md 참고).
 */
public record ScoreEarnEvent(Long userId, ScoreReason reason, Long reviewId) {
}
