package com.na.naknak.server.raffle.domain.repository;

import com.na.naknak.server.raffle.domain.Raffle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RaffleRepository extends JpaRepository<Raffle, Long> {

    List<Raffle> findAllByOrderByCreatedAtDesc();
}
