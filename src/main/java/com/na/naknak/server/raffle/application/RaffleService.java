package com.na.naknak.server.raffle.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.raffle.domain.Raffle;
import com.na.naknak.server.raffle.domain.RaffleEntry;
import com.na.naknak.server.raffle.domain.RaffleStatus;
import com.na.naknak.server.raffle.domain.repository.RaffleEntryRepository;
import com.na.naknak.server.raffle.domain.repository.RaffleRepository;
import com.na.naknak.server.raffle.presentation.dto.MyEntryResponse;
import com.na.naknak.server.raffle.presentation.dto.RaffleDetailResponse;
import com.na.naknak.server.raffle.presentation.dto.RaffleSummaryResponse;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.domain.ScoreReason;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RaffleService {

    private final RaffleRepository raffleRepository;
    private final RaffleEntryRepository raffleEntryRepository;
    private final ScoreService scoreService;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public List<RaffleSummaryResponse> getList() {
        return raffleRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(RaffleSummaryResponse::from)
                .toList();
    }

    @Transactional
    public RaffleDetailResponse getDetail(Long userId, Long raffleId) {
        Raffle raffle = getOrThrow(raffleId);
        drawIfDue(raffle);
        return toDetail(raffle, userId);
    }

    @Transactional
    public MyEntryResponse enter(Long userId, Long raffleId) {
        Raffle raffle = getOrThrow(raffleId);
        drawIfDue(raffle);
        if (raffle.getStatus() != RaffleStatus.OPEN) {
            throw new BusinessException(ErrorCode.RAFFLE_CLOSED);
        }

        scoreService.spendPoints(userId, raffle.getPointCostPerEntry(), ScoreReason.RAFFLE_ENTRY);

        RaffleEntry entry = raffleEntryRepository.findByRaffleIdAndUserId(raffleId, userId)
                .orElseGet(() -> raffleEntryRepository.save(RaffleEntry.create(raffleId, userId)));
        entry.addEntry();

        return new MyEntryResponse(raffleId, entry.getCount());
    }

    private void drawIfDue(Raffle raffle) {
        if (!raffle.isRevealDue()) {
            return;
        }
        List<RaffleEntry> entries = raffleEntryRepository.findByRaffleId(raffle.getId()).stream()
                .filter(entry -> entry.getCount() > 0)
                .toList();
        raffle.draw(pickWeightedWinner(entries));
    }

    private Long pickWeightedWinner(List<RaffleEntry> entries) {
        int totalEntries = entries.stream().mapToInt(RaffleEntry::getCount).sum();
        if (totalEntries == 0) {
            return null;
        }
        int ticket = random.nextInt(totalEntries);
        int cumulative = 0;
        for (RaffleEntry entry : entries) {
            cumulative += entry.getCount();
            if (ticket < cumulative) {
                return entry.getUserId();
            }
        }
        return entries.get(entries.size() - 1).getUserId();
    }

    private Raffle getOrThrow(Long raffleId) {
        return raffleRepository.findById(raffleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RAFFLE_NOT_FOUND));
    }

    private RaffleDetailResponse toDetail(Raffle raffle, Long requestUserId) {
        int myEntryCount = raffleEntryRepository.findByRaffleIdAndUserId(raffle.getId(), requestUserId)
                .map(RaffleEntry::getCount)
                .orElse(0);

        boolean isWinner = requestUserId.equals(raffle.getWinnerUserId());
        String winnerNickname = Optional.ofNullable(raffle.getWinnerUserId())
                .flatMap(userRepository::findById)
                .map(User::getNickname)
                .orElse(null);

        return new RaffleDetailResponse(
                raffle.getId(),
                raffle.getTitle(),
                raffle.getPrizeName(),
                raffle.getPrizeAmountKrw(),
                raffle.getPointCostPerEntry(),
                raffle.getRevealAt(),
                raffle.getStatus().name(),
                myEntryCount,
                winnerNickname,
                isWinner,
                isWinner ? raffle.getGiftCode() : null
        );
    }
}
