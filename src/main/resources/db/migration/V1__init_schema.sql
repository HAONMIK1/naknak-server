-- ========================================
-- 낙낙(NakNak) 초기 스키마 v1
-- ========================================

-- 1. users
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    kakao_id    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100),
    nickname    VARCHAR(30)  NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_user_kakao_id ON users (kakao_id);
CREATE INDEX idx_user_nickname ON users (nickname);

-- 2. invite_codes
CREATE TABLE invite_codes (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    created_by  BIGINT       NOT NULL REFERENCES users (id),
    used_by     BIGINT       REFERENCES users (id),
    used_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- 3. follows
CREATE TABLE follows (
    id           BIGSERIAL PRIMARY KEY,
    follower_id  BIGINT NOT NULL REFERENCES users (id),
    following_id BIGINT NOT NULL REFERENCES users (id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (follower_id, following_id)
);

CREATE INDEX idx_follows_follower   ON follows (follower_id);
CREATE INDEX idx_follows_following  ON follows (following_id);

-- 4. restaurants
CREATE TABLE restaurants (
    id               BIGSERIAL PRIMARY KEY,
    naver_place_id   VARCHAR(50)   UNIQUE,
    naver_place_url  VARCHAR(500),
    name             VARCHAR(100)  NOT NULL,
    category         VARCHAR(50)   NOT NULL,
    address          VARCHAR(200)  NOT NULL,
    latitude         DOUBLE PRECISION,
    longitude        DOUBLE PRECISION,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurant_naver_place_id ON restaurants (naver_place_id);
CREATE INDEX idx_restaurant_name           ON restaurants (name);
CREATE INDEX idx_restaurant_category       ON restaurants (category);

-- 5. reviews
CREATE TABLE reviews (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id),
    restaurant_id  BIGINT      NOT NULL REFERENCES restaurants (id),
    content        TEXT        NOT NULL,
    rating         SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMP
);

CREATE INDEX idx_review_user_id       ON reviews (user_id);
CREATE INDEX idx_review_restaurant_id ON reviews (restaurant_id);

-- 6. review_images
CREATE TABLE review_images (
    id          BIGSERIAL PRIMARY KEY,
    review_id   BIGINT        NOT NULL REFERENCES reviews (id),
    image_url   VARCHAR(500)  NOT NULL,
    sort_order  INT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_review_images_review_id ON review_images (review_id);

-- 7. user_score
CREATE TABLE user_score (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL UNIQUE REFERENCES users (id),
    total_score  INT    NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 8. user_score_history
CREATE TABLE user_score_history (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    delta       INT         NOT NULL,
    reason      VARCHAR(50) NOT NULL,
    review_id   BIGINT      REFERENCES reviews (id),
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_score_history_user_id ON user_score_history (user_id);

-- 9. ranking
CREATE TABLE ranking (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES users (id),
    period_type  VARCHAR(10) NOT NULL,
    period_key   VARCHAR(20) NOT NULL,
    score        INT         NOT NULL DEFAULT 0,
    rank         INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ranking_period ON ranking (period_type, period_key);

-- 10. saved_restaurants (북마크)
CREATE TABLE saved_restaurants (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (id),
    restaurant_id  BIGINT NOT NULL REFERENCES restaurants (id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, restaurant_id)
);

-- 11. restaurant_thanks (감사하기)
CREATE TABLE restaurant_thanks (
    id             BIGSERIAL PRIMARY KEY,
    restaurant_id  BIGINT NOT NULL REFERENCES restaurants (id),
    sender_id      BIGINT NOT NULL REFERENCES users (id),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (restaurant_id, sender_id)
);

-- 12. notifications (알림)
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id),
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(100) NOT NULL,
    body        VARCHAR(300) NOT NULL,
    link        VARCHAR(200),
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id, is_read);