package com.na.naknak.server.score.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 점수/포인트 적립·차감 사유. 각 사유가 점수/포인트에 얼마씩 반영되는지 함께 갖는다. */
@Getter
@RequiredArgsConstructor
public enum ScoreReason {

    REVIEW_CREATE(ScorePolicy.REVIEW_CREATE_SCORE, ScorePolicy.REVIEW_CREATE_POINT),
    REVIEW_PHOTO(ScorePolicy.REVIEW_PHOTO_SCORE, ScorePolicy.REVIEW_PHOTO_POINT),
    INVITE(ScorePolicy.INVITE_SCORE, ScorePolicy.INVITE_POINT);

    private final int scoreDelta;
    private final int pointDelta;
}
