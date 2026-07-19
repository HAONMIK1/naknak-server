package com.na.naknak.server.user.presentation.dto;

import com.na.naknak.server.user.domain.User;

public record MyProfileResponse(
        Long id,
        String nickname,
        String email,
        String inviteCode,
        long followerCount,
        long followingCount
) {
    public static MyProfileResponse from(User user, String inviteCode, long followerCount, long followingCount) {
        return new MyProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                inviteCode,
                followerCount,
                followingCount
        );
    }
}