package com.na.naknak.server.feed.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.feed.application.FeedService;
import com.na.naknak.server.review.presentation.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/api/v1/feed/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getFeed(
            @LoginUser Long userId,
            @RequestParam(required = false) List<Integer> degrees,
            Pageable pageable
    ) {
        Set<Integer> degreeSet = degrees == null ? Set.of() : new HashSet<>(degrees);
        return ResponseEntity.ok(ApiResponse.ok(feedService.getFeed(userId, degreeSet, pageable)));
    }
}
