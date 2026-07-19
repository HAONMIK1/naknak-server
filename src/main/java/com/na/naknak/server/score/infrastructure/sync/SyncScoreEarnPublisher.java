package com.na.naknak.server.score.infrastructure.sync;

import com.na.naknak.server.score.application.ScoreEarnEvent;
import com.na.naknak.server.score.application.ScoreEarnPublisher;
import com.na.naknak.server.score.application.ScoreService;
import lombok.RequiredArgsConstructor;

/**
 * SQS 큐가 설정되지 않은 환경(로컬/테스트)의 기본 구현 — 큐 없이 그 자리에서 바로
 * {@link ScoreService#earn}을 호출한다. 빈 등록은 {@link ScoreEarnPublisherConfig}가
 * 담당한다(플레인 @Component로 등록하면 SqsScoreEarnPublisher와의 @ConditionalOnMissingBean
 * 평가 순서가 보장되지 않는다).
 */
@RequiredArgsConstructor
public class SyncScoreEarnPublisher implements ScoreEarnPublisher {

    private final ScoreService scoreService;

    @Override
    public void publish(ScoreEarnEvent event) {
        scoreService.earn(event.userId(), event.reason(), event.reviewId());
    }
}
