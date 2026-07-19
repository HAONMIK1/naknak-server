package com.na.naknak.server.score.application;

/**
 * 점수 적립 요청을 발행한다. 운영 환경(SQS 큐 설정됨)에서는 큐에 던지고 별도 컨슈머가
 * 처리하며, 큐가 없는 로컬/테스트 환경에서는 그 자리에서 바로 처리한다 — 두 구현이 같은
 * 인터페이스라 호출부는 환경을 구분하지 않는다 (docs/score.md 참고).
 */
public interface ScoreEarnPublisher {

    void publish(ScoreEarnEvent event);
}
