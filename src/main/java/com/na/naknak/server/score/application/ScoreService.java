package com.na.naknak.server.score.application;

import com.na.naknak.server.follow.domain.repository.FollowRepository;
import com.na.naknak.server.review.domain.repository.ReviewRepository;
import com.na.naknak.server.score.domain.RankingScope;
import com.na.naknak.server.score.domain.ScoreReason;
import com.na.naknak.server.score.domain.ScoreTarget;
import com.na.naknak.server.score.domain.UserScore;
import com.na.naknak.server.score.domain.UserScoreHistory;
import com.na.naknak.server.score.domain.repository.UserScoreHistoryRepository;
import com.na.naknak.server.score.domain.repository.UserScoreRepository;
import com.na.naknak.server.score.infrastructure.redis.GlobalRankingCache;
import com.na.naknak.server.score.presentation.dto.LocalRankingEntryResponse;
import com.na.naknak.server.score.presentation.dto.LocalRankingResponse;
import com.na.naknak.server.score.presentation.dto.RankingEntryResponse;
import com.na.naknak.server.score.presentation.dto.RankingResponse;
import com.na.naknak.server.score.presentation.dto.WalletResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.na.naknak.server.score.domain.ScorePolicy.RANKING_TOP_N;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private static final int MAX_NETWORK_DEGREE = 3;
    private static final int MAX_NETWORK_MEMBERS = 500;

    private final UserScoreRepository userScoreRepository;
    private final UserScoreHistoryRepository userScoreHistoryRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;
    private final GlobalRankingCache globalRankingCache;

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

        if (reason.getScoreDelta() != 0) {
            globalRankingCache.addScore(userId, reason.getScoreDelta());
        }
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
    public RankingResponse getRanking(Long userId, RankingScope scope) {
        return scope == RankingScope.NETWORK ? getNetworkRanking(userId) : getGlobalRanking(userId);
    }

    private RankingResponse getGlobalRanking(Long userId) {
        ensureGlobalRankingSeeded();

        List<ScoredEntry<Long>> top = new ArrayList<>(globalRankingCache.getTop(RANKING_TOP_N));
        Map<Long, User> usersById = usersById(top.stream().map(ScoredEntry::getValue).toList());

        List<RankingEntryResponse> topEntries = new ArrayList<>();
        int rank = 1;
        for (ScoredEntry<Long> entry : top) {
            User user = usersById.get(entry.getValue());
            String nickname = user != null ? user.getNickname() : "알 수 없음";
            topEntries.add(new RankingEntryResponse(entry.getValue(), nickname, entry.getScore().intValue(), rank++));
        }

        int myRank = globalRankingCache.getRank(userId).map(r -> r + 1)
                .orElseGet(() -> {
                    int myScore = userScoreRepository.findByUserId(userId).map(UserScore::getTotalScore).orElse(0);
                    return (int) userScoreRepository.countByTotalScoreGreaterThan(myScore) + 1;
                });

        return new RankingResponse(topEntries, myRank);
    }

    /**
     * Redis ZSET이 비어있으면(콜드스타트) DB 전체를 1회 backfill한다. 이후로는 earn()의
     * 증분 갱신만으로 정합성을 유지한다(docs/score.md 참고).
     */
    private void ensureGlobalRankingSeeded() {
        if (globalRankingCache.isSeeded()) {
            return;
        }
        Map<Long, Integer> scoresByUserId = userScoreRepository.findAllByOrderByTotalScoreDesc().stream()
                .collect(Collectors.toMap(UserScore::getUserId, UserScore::getTotalScore));
        globalRankingCache.backfill(scoresByUserId);
    }

    private RankingResponse getNetworkRanking(Long userId) {
        List<Long> networkUserIds = followRepository
                .findNetworkDegrees(userId, MAX_NETWORK_DEGREE, MAX_NETWORK_MEMBERS).stream()
                .map(FollowRepository.NetworkDegreeRow::getUserId)
                .toList();

        if (networkUserIds.isEmpty()) {
            return new RankingResponse(List.of(), 1);
        }

        List<Long> scopedIds = new ArrayList<>(networkUserIds);
        scopedIds.add(userId);

        List<UserScore> scoped = userScoreRepository.findByUserIdIn(scopedIds).stream()
                .sorted(Comparator.comparingInt(UserScore::getTotalScore).reversed())
                .toList();

        Map<Long, User> usersById = usersById(scoped.stream().map(UserScore::getUserId).toList());

        List<RankingEntryResponse> topEntries = IntStream.range(0, Math.min(scoped.size(), RANKING_TOP_N))
                .mapToObj(i -> {
                    UserScore userScore = scoped.get(i);
                    User user = usersById.get(userScore.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return new RankingEntryResponse(userScore.getUserId(), nickname, userScore.getTotalScore(), i + 1);
                })
                .toList();

        int myScore = userScoreRepository.findByUserId(userId).map(UserScore::getTotalScore).orElse(0);
        long myRank = scoped.stream().filter(s -> s.getTotalScore() > myScore).count() + 1;

        return new RankingResponse(topEntries, (int) myRank);
    }

    @Transactional(readOnly = true)
    public LocalRankingResponse getLocalRanking(Long userId, String region) {
        List<ReviewRepository.RegionReviewCountRow> top = reviewRepository.countReviewsByRegion(region, RANKING_TOP_N);

        Map<Long, User> usersById = usersById(
                top.stream().map(ReviewRepository.RegionReviewCountRow::getUserId).toList());

        List<LocalRankingEntryResponse> topEntries = IntStream.range(0, top.size())
                .mapToObj(i -> {
                    ReviewRepository.RegionReviewCountRow row = top.get(i);
                    User user = usersById.get(row.getUserId());
                    String nickname = user != null ? user.getNickname() : "알 수 없음";
                    return new LocalRankingEntryResponse(row.getUserId(), nickname, row.getReviewCount(), i + 1);
                })
                .toList();

        long myReviewCount = reviewRepository.countMyReviewsByRegion(userId, region);
        Integer myRank = myReviewCount == 0
                ? null
                : (int) reviewRepository.countUsersAheadInRegion(region, myReviewCount) + 1;

        return new LocalRankingResponse(topEntries, myReviewCount, myRank);
    }

    private Map<Long, User> usersById(List<Long> userIds) {
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}
