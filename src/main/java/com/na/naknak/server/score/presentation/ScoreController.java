package com.na.naknak.server.score.presentation;

import com.na.naknak.server.common.ApiResponse;
import com.na.naknak.server.common.auth.LoginUser;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.presentation.dto.RankingResponse;
import com.na.naknak.server.score.presentation.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    public ResponseEntity<ApiResponse<RankingResponse>> getRanking(@LoginUser Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(scoreService.getRanking(userId)));
    }
}
