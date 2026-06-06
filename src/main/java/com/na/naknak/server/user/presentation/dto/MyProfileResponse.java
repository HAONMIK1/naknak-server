package com.na.naknak.server.user.presentation.dto;

import com.na.naknak.server.user.domain.User;

public record MyProfileResponse(
        Long id,
        String nickname,
        String email,
        String inviteCode
) {
    public static MyProfileResponse from(User user, String inviteCode) {
        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                inviteCode
        );
    }
}