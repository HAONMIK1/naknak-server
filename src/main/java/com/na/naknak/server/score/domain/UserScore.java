package com.na.naknak.server.score.domain;

import com.na.naknak.server.common.BaseTimeEntity;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 유저당 1행인 점수/포인트 지갑.
 * totalScore(랭킹용, 누적만 됨)와 pointBalance(지갑, 차감 가능)를 분리해서 갖는다.
 */
@Getter
@Entity
@Table(name = "user_score")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserScore extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "point_balance", nullable = false)
    private int pointBalance;

    public static UserScore create(Long userId) {
        UserScore userScore = new UserScore();
        userScore.userId = userId;
        userScore.totalScore = 0;
        userScore.pointBalance = 0;
        return userScore;
    }

    public void addScore(int amount) {
        this.totalScore += amount;
    }

    public void addPoints(int amount) {
        this.pointBalance += amount;
    }

    public void spendPoints(int amount) {
        if (this.pointBalance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.pointBalance -= amount;
    }
}
