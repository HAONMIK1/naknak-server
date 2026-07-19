package com.na.naknak.server.score.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.common.exception.ErrorCode;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.domain.RankingScope;
import com.na.naknak.server.score.presentation.dto.LocalRankingResponse;
import com.na.naknak.server.score.presentation.dto.RankingResponse;
import com.na.naknak.server.score.presentation.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    @GetMapping("/api/v1/me/wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@LoginUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(scoreService.getMyWallet(userId)));
    }

    @GetMapping("/api/v1/ranking")
    public ResponseEntity<ApiResponse<RankingResponse>> getRanking(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "GLOBAL") RankingScope scope
    ) {
        return ResponseEntity.ok(ApiResponse.ok(scoreService.getRanking(userId, scope)));
    }

    @GetMapping("/api/v1/ranking/local")
    public ResponseEntity<ApiResponse<LocalRankingResponse>> getLocalRanking(
            @LoginUser Long userId,
            @RequestParam String region
    ) {
        if (region.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return ResponseEntity.ok(ApiResponse.ok(scoreService.getLocalRanking(userId, region)));
    }
}
