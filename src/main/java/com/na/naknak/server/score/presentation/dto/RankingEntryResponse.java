package com.na.naknak.server.score.presentation.dto;

public record RankingEntryResponse(Long userId, String nickname, int totalScore, int rank) {
}
