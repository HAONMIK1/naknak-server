-- 맛집 대표 사진 (네이버 이미지 검색 결과, 등록 시점에 최대 3장 수집)
CREATE TABLE restaurant_images (
    id             BIGSERIAL PRIMARY KEY,
    restaurant_id  BIGINT        NOT NULL REFERENCES restaurants (id),
    image_url      VARCHAR(500)  NOT NULL,
    sort_order     INT           NOT NULL DEFAULT 0,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_restaurant_images_restaurant_id ON restaurant_images (restaurant_id);
