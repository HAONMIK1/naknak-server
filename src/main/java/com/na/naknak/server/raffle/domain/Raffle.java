package com.na.naknak.server.raffle.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * raffles 테이블은 created_at 만 갖는다(updated_at/deleted_at 없음).
 * 따라서 BaseTimeEntity 대신 자체 감사 필드를 둔다 (follows, review_images와 동일한 패턴).
 */
@Getter
@Entity
@Table(name = "raffles")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Raffle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "prize_name", nullable = false, length = 100)
    private String prizeName;

    @Column(name = "prize_amount_krw", nullable = false)
    private int prizeAmountKrw;

    @Column(name = "point_cost_per_entry", nullable = false)
    private int pointCostPerEntry;

    @Column(name = "reveal_at", nullable = false)
    private LocalDateTime revealAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RaffleStatus status;

    @Column(name = "winner_user_id")
    private Long winnerUserId;

    @Column(name = "gift_code", length = 200)
    private String giftCode;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public static Raffle create(String title, String prizeName, int prizeAmountKrw, int pointCostPerEntry, LocalDateTime revealAt) {
        Raffle raffle = new Raffle();
        raffle.title = title;
        raffle.prizeName = prizeName;
        raffle.prizeAmountKrw = prizeAmountKrw;
        raffle.pointCostPerEntry = pointCostPerEntry;
        raffle.revealAt = revealAt;
        raffle.status = RaffleStatus.OPEN;
        return raffle;
    }

    /** reveal_at이 지났는데 아직 추첨 전인지 — 지연 추첨을 트리거해야 하는 시점인지 판단한다. */
    public boolean isRevealDue() {
        return status == RaffleStatus.OPEN && !LocalDateTime.now().isBefore(revealAt);
    }

    /** winnerUserId가 null이면 참여자가 없어 유찰된 것이다. */
    public void draw(Long winnerUserId) {
        this.winnerUserId = winnerUserId;
        this.status = RaffleStatus.DRAWN;
    }
}
