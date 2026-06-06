package com.na.naknak.server.user.domain.repository;

import com.na.naknak.server.user.domain.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    Optional<InviteCode> findByCodeAndUsedByIsNull(String code);

    boolean existsByCode(String newCode);
}