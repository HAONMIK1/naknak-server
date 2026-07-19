package com.na.naknak.server.raffle.presentation.dto;

import java.time.LocalDateTime;

public record RaffleDetailResponse(
        Long id,
        String title,
        String prizeName,
        int prizeAmountKrw,
        int pointCostPerEntry,
        LocalDateTime revealAt,
        String status,
        int myEntryCount,
        String winnerNickname,
        boolean isWinner,
        String giftCode
) {
}
