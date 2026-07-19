package com.na.naknak.server.score.presentation.dto;

import java.util.List;

/**
 * myRank는 내가 이 지역에 리뷰를 하나도 안 남겼으면 null이다(0등이 아니라 "아직 순위 없음").
 */
public record LocalRankingResponse(List<LocalRankingEntryResponse> topEntries, long myReviewCount, Integer myRank) {
}
