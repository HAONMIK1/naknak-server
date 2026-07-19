package com.na.naknak.server.raffle.domain.repository;

import com.na.naknak.server.raffle.domain.RaffleEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RaffleEntryRepository extends JpaRepository<RaffleEntry, Long> {

    Optional<RaffleEntry> findByRaffleIdAndUserId(Long raffleId, Long userId);

    List<RaffleEntry> findByRaffleId(Long raffleId);
}
