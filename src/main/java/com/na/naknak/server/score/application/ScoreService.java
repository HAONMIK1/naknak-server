package com.na.naknak.server.score.application;

import com.na.naknak.server.score.domain.ScoreReason;
import com.na.naknak.server.score.domain.ScoreTarget;
import com.na.naknak.server.score.domain.UserScore;
import com.na.naknak.server.score.domain.UserScoreHistory;
import com.na.naknak.server.score.domain.repository.UserScoreHistoryRepository;
import com.na.naknak.server.score.domain.repository.UserScoreRepository;
import com.na.naknak.server.score.presentation.dto.RankingEntryResponse;
import com.na.naknak.server.score.presentation.dto.RankingResponse;
import com.na.naknak.server.score.presentation.dto.WalletResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.na.naknak.server.score.domain.ScorePolicy.RANKING_TOP_N;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final UserScoreRepository userScoreRepository;
    private final UserScoreHistoryRepository userScoreHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public void earn(Long userId, ScoreReason reason) {
        earn(userId, reason, null);
    }

    @Transactional
    public void earn(Long userId, ScoreReason reason, Long reviewId) {
        UserScore userScore = userScoreRepository.findByUserId(userId)
                .orElseGet(() -> userScoreRepository.save(UserScore.create(userId)));

        userScore.addScore(reason.getScoreDelta());
        userScore.addPoints(reason.getPointDelta());

        userScoreHistoryRepository.save(
                UserScoreHistory.create(userId, ScoreTarget.SCORE, reason.getScoreDelta(), reason, reviewId));
        userScoreHistoryRepository.save(
                UserScoreHistory.create(userId, ScoreTarget.POINT, reason.getPointDelta(), reason, reviewId));
    }

    @Transactional
    public void spendPoints(Long userId, int amount, ScoreReason reason) {
        UserScore userScore = userScoreRepository.findByUserId(userId)
                .orElseGet(() -> userScoreRepository.save(UserScore.create(userId)));
        userScore.spendPoints(amount);
        userScoreHistoryRepository.save(
                UserScoreHistory.create(userId, ScoreTarget.POINT, -amount, reason, null));
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(Long userId) {
        return userScoreRepository.findByUserId(userId)
                .map(userScore -> new WalletResponse(userScore.getTotalScore(), userScore.getPointBalance()))
                .orElseGet(WalletResponse::empty);
    }

    @Transactional(readOnly = true)
    public RankingResponse getRanking(Long userId) {
        List<UserScore> top = userScoreRepository.findByOrderByTotalScoreDesc(PageRequest.of(0, RANKING_TOP_N));

        Map<Long, User> usersById = userRepository.findAllById(
                top.stream().map(UserScore::getUserId).toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<RankingEntryResponse> topEntries = IntStream.range(0, top.size())
                .mapToObj(i -> {
                    UserScore userScore = top.get(i);
                    User user = usersById.get(userScore.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return new RankingEntryResponse(userScore.getUserId(), nickname, userScore.getTotalScore(), i + 1);
                })
                .toList();

        int myScore = userScoreRepository.findByUserId(userId).map(UserScore::getTotalScore).orElse(0);
        int myRank = (int) userScoreRepository.countByTotalScoreGreaterThan(myScore) + 1;

        return new RankingResponse(topEntries, myRank);
    }
}
