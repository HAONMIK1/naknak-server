-- 복권(래플): 관리자가 상품 이벤트를 열면 유저가 포인트로 응모한다
CREATE TABLE raffles (
    id                     BIGSERIAL PRIMARY KEY,
    title                  VARCHAR(100) NOT NULL,
    prize_name             VARCHAR(100) NOT NULL,
    prize_amount_krw       INT          NOT NULL,
    point_cost_per_entry   INT          NOT NULL,
    reveal_at              TIMESTAMP    NOT NULL,
    status                 VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    winner_user_id         BIGINT       REFERENCES users (id),
    gift_code              VARCHAR(200),
    created_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE raffle_entries (
    id          BIGSERIAL PRIMARY KEY,
    raffle_id   BIGINT NOT NULL REFERENCES raffles (id),
    user_id     BIGINT NOT NULL REFERENCES users (id),
    count       INT    NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_raffle_entry_raffle_user UNIQUE (raffle_id, user_id)
);

CREATE INDEX idx_raffle_entries_raffle_id ON raffle_entries (raffle_id);
