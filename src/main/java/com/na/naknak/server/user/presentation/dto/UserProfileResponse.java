package com.na.naknak.server.user.presentation.dto;

import com.na.naknak.server.user.domain.User;

public record UserProfileResponse(
        Long id,
        String nickname,
        String email
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail()
        );
    }
}