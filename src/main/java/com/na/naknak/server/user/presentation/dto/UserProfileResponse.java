package com.na.naknak.server.user.presentation.dto;

import com.na.naknak.server.user.domain.User;

public record UserProfileResponse(
        Long id,
        String nickname,
        String email,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getNickname(), user.getEmail(), 0, 0, false);
    }

    public static UserProfileResponse from(User user, long followerCount, long followingCount, boolean isFollowing) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                followerCount,
                followingCount,
                isFollowing
        );
    }
}