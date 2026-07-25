package com.na.naknak.server.raffle.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.raffle.application.RaffleService;
import com.na.naknak.server.raffle.presentation.dto.MyEntryResponse;
import com.na.naknak.server.raffle.presentation.dto.RaffleDetailResponse;
import com.na.naknak.server.raffle.presentation.dto.RaffleSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/raffles")
@RequiredArgsConstructor
public class RaffleController {

    private final RaffleService raffleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RaffleSummaryResponse>>> getList() {
        return ResponseEntity.ok(ApiResponse.ok(raffleService.getList()));
    }

    @GetMapping("/{raffleId}")
    public ResponseEntity<ApiResponse<RaffleDetailResponse>> getDetail(
            @LoginUser Long userId,
            @PathVariable Long raffleId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(raffleService.getDetail(userId, raffleId)));
    }

    @PostMapping("/{raffleId}/entries")
    public ResponseEntity<ApiResponse<MyEntryResponse>> enter(
            @LoginUser Long userId,
            @PathVariable Long raffleId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(raffleService.enter(userId, raffleId)));
    }
}
