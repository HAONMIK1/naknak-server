-- 점수(랭킹용, 누적)와 포인트(지갑, 차감 가능)를 분리하기 위한 컬럼 추가
ALTER TABLE user_score
    ADD COLUMN point_balance INT NOT NULL DEFAULT 0;

ALTER TABLE user_score_history
    ADD COLUMN target VARCHAR(10) NOT NULL DEFAULT 'SCORE';
