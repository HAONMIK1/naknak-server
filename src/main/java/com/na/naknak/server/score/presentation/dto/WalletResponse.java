package com.na.naknak.server.score.presentation.dto;

public record WalletResponse(int totalScore, int pointBalance) {

    public static WalletResponse empty() {
        return new WalletResponse(0, 0);
    }
}
