package com.na.naknak.server.score.presentation.dto;

import java.util.List;

/**
 * 상위 N명 목록과 별개로 myRank를 항상 내려준다 — 내가 상위 N명 밖이어도
 * 화면에서 "내 순위: 128등" 같은 안내가 가능하게 하기 위함.
 */
public record RankingResponse(List<RankingEntryResponse> topEntries, int myRank) {
}
