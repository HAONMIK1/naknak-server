package com.na.naknak.server.score.domain;

/**
 * 적립 정책 상수 모음. 정책이 바뀌어도 이 파일만 보면 되게 한 곳에 모아둔다.
 * (docs/score.md의 "적립 정책" 표와 반드시 일치시킬 것)
 */
public final class ScorePolicy {

    private ScorePolicy() {
    }

    public static final int REVIEW_CREATE_SCORE = 10;
    public static final int REVIEW_CREATE_POINT = 10;

    public static final int REVIEW_PHOTO_SCORE = 5;
    public static final int REVIEW_PHOTO_POINT = 5;

    public static final int INVITE_SCORE = 20;
    public static final int INVITE_POINT = 20;

    public static final int RANKING_TOP_N = 50;
}
