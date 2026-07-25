package com.na.naknak.server.follow.presentation.dto;

import com.na.naknak.server.user.domain.User;

public record FollowUserResponse(Long id, String nickname) {
    public static FollowUserResponse from(User user) {
        return new FollowUserResponse(user.getId(), user.getNickname());
    }
}
