package com.na.naknak.server.user.domain.repository;

import com.na.naknak.server.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKakaoId(String kakaoId);

    Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String nickname);

    List<User> findByNicknameContaining(String keyword);
}