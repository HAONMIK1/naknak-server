package com.na.naknak.server.score.presentation.dto;

public record LocalRankingEntryResponse(Long userId, String nickname, long reviewCount, int rank) {
}
