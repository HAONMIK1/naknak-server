package com.na.naknak.server.user.domain;

import com.na.naknak.server.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "invite_codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    public static InviteCode create(String code, User createdBy) {
        InviteCode inviteCode = new InviteCode();
        inviteCode.code = code;
        inviteCode.createdBy = createdBy;
        return inviteCode;
    }

    public boolean isUsed() {
        return usedBy != null;
    }

    public void use(User user) {
        this.usedBy = user;
        this.usedAt = LocalDateTime.now();
    }
}