package com.na.naknak.server.raffle.domain;

import com.na.naknak.server.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 래플 하나당 유저 하나가 1행 — 같은 래플에 여러 번 응모하면 count가 누적된다
 * (엔트리 row가 여러 개 생기지 않음). count가 바뀌는 값이라 BaseTimeEntity(updated_at 포함)를 쓴다.
 */
@Getter
@Entity
@Table(
        name = "raffle_entries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_raffle_entry_raffle_user",
                columnNames = {"raffle_id", "user_id"}
        ),
        indexes = @Index(name = "idx_raffle_entries_raffle_id", columnList = "raffle_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raffle_id", nullable = false)
    private Long raffleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int count;

    public static RaffleEntry create(Long raffleId, Long userId) {
        RaffleEntry entry = new RaffleEntry();
        entry.raffleId = raffleId;
        entry.userId = userId;
        entry.count = 0;
        return entry;
    }

    public void addEntry() {
        this.count += 1;
    }
}
