package com.na.naknak.server.raffle.presentation.dto;

import com.na.naknak.server.raffle.domain.Raffle;

import java.time.LocalDateTime;

public record RaffleSummaryResponse(
        Long id,
        String title,
        String prizeName,
        int prizeAmountKrw,
        int pointCostPerEntry,
        LocalDateTime revealAt,
        String status
) {
    public static RaffleSummaryResponse from(Raffle raffle) {
        return new RaffleSummaryResponse(
                raffle.getId(),
                raffle.getTitle(),
                raffle.getPrizeName(),
                raffle.getPrizeAmountKrw(),
                raffle.getPointCostPerEntry(),
                raffle.getRevealAt(),
                raffle.getStatus().name()
        );
    }
}
