package com.na.naknak.server.user.domain;

import com.na.naknak.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_kakao_id", columnList = "kakao_id"),
                @Index(name = "idx_user_nickname", columnList = "nickname")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private String kakaoId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    public static User create(String kakaoId, String email, String nickname) {
        User user = new User();
        user.kakaoId = kakaoId;
        user.email = email;
        user.nickname = nickname;
        return user;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

}