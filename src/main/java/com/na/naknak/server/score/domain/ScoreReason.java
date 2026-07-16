package com.na.naknak.server.score.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 점수/포인트 적립·차감 사유. 각 사유가 점수/포인트에 얼마씩 반영되는지 함께 갖는다. */
@Getter
@RequiredArgsConstructor
public enum ScoreReason {

    REVIEW_CREATE(ScorePolicy.REVIEW_CREATE_SCORE, ScorePolicy.REVIEW_CREATE_POINT),
    REVIEW_PHOTO(ScorePolicy.REVIEW_PHOTO_SCORE, ScorePolicy.REVIEW_PHOTO_POINT),
    INVITE(ScorePolicy.INVITE_SCORE, ScorePolicy.INVITE_POINT),
    // 차감 전용 사유 — spendPoints()에서만 쓰이므로 델타는 의미 없다(0으로 둔다).
    RAFFLE_ENTRY(0, 0);

    private final int scoreDelta;
    private final int pointDelta;
}
