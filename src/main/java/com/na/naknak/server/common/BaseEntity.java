package com.na.naknak.server.common;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 생성/수정 시각 + 소프트 딜리트(deleted_at)를 갖는 감사 상위 클래스.
 * deleted_at 컬럼이 있는 테이블(users, reviews 등)이 상속한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}