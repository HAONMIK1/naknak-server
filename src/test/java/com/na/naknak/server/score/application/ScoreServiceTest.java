package com.na.naknak.server.score.application;

import com.na.naknak.server.common.exception.BusinessException;
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
import com.na.naknak.server.score.presentation.dto.LocalRankingResponse;
import com.na.naknak.server.score.presentation.dto.RankingResponse;
import com.na.naknak.server.score.presentation.dto.WalletResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @InjectMocks
    private ScoreService scoreService;

    @Mock
    private UserScoreRepository userScoreRepository;
    @Mock
    private UserScoreHistoryRepository userScoreHistoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private GlobalRankingCache globalRankingCache;

    private UserScore userScoreWithId(Long userId, int totalScore, int pointBalance) {
        UserScore userScore = UserScore.create(userId);
        ReflectionTestUtils.setField(userScore, "totalScore", totalScore);
        ReflectionTestUtils.setField(userScore, "pointBalance", pointBalance);
        return userScore;
    }

    private User userWithId(Long id, String nickname) {
        User user = User.create(String.valueOf(id), "u" + id + "@test.com", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private FollowRepository.NetworkDegreeRow networkRow(Long userId, int degree) {
        return new FollowRepository.NetworkDegreeRow() {
            public Long getUserId() {
                return userId;
            }

            public Integer getDegree() {
                return degree;
            }
        };
    }

    private ReviewRepository.RegionReviewCountRow regionRow(Long userId, long count) {
        return new ReviewRepository.RegionReviewCountRow() {
            public Long getUserId() {
                return userId;
            }

            public Long getReviewCount() {
                return count;
            }
        };
    }

    @Test
    void 처음_적립하면_지갑이_0에서_생성되어_반영된다() {
        // given
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.empty());
        given(userScoreRepository.save(any(UserScore.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        scoreService.earn(1L, ScoreReason.REVIEW_CREATE);

        // then
        ArgumentCaptor<UserScore> captor = ArgumentCaptor.forClass(UserScore.class);
        verify(userScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalScore()).isEqualTo(10);
        assertThat(captor.getValue().getPointBalance()).isEqualTo(10);

        ArgumentCaptor<UserScoreHistory> historyCaptor = ArgumentCaptor.forClass(UserScoreHistory.class);
        verify(userScoreHistoryRepository, times(2)).save(historyCaptor.capture());
        assertThat(historyCaptor.getAllValues())
                .extracting(UserScoreHistory::getTarget)
                .containsExactlyInAnyOrder(ScoreTarget.SCORE, ScoreTarget.POINT);

        verify(globalRankingCache).addScore(1L, 10);
    }

    @Test
    void 기존_지갑에_적립하면_누적된다() {
        // given
        UserScore existing = userScoreWithId(1L, 100, 50);
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        // when
        scoreService.earn(1L, ScoreReason.REVIEW_PHOTO, 999L);

        // then
        assertThat(existing.getTotalScore()).isEqualTo(105);
        assertThat(existing.getPointBalance()).isEqualTo(55);
        verify(globalRankingCache).addScore(1L, 5);
    }

    @Test
    void 포인트_차감_성공() {
        // given
        UserScore existing = userScoreWithId(1L, 100, 50);
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        // when
        scoreService.spendPoints(1L, 30, ScoreReason.REVIEW_CREATE);

        // then
        assertThat(existing.getPointBalance()).isEqualTo(20);
        assertThat(existing.getTotalScore()).isEqualTo(100);
    }

    @Test
    void 포인트_부족하면_차감_예외() {
        // given
        UserScore existing = userScoreWithId(1L, 100, 10);
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> scoreService.spendPoints(1L, 30, ScoreReason.REVIEW_CREATE))
                .isInstanceOf(BusinessException.class);
        assertThat(existing.getPointBalance()).isEqualTo(10);
    }

    @Test
    void 지갑_조회_활동없으면_0으로_반환() {
        // given
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.empty());

        // when
        WalletResponse wallet = scoreService.getMyWallet(1L);

        // then
        assertThat(wallet.totalScore()).isEqualTo(0);
        assertThat(wallet.pointBalance()).isEqualTo(0);
    }

    @Test
    void 지갑_조회_기존값_반환() {
        // given
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(userScoreWithId(1L, 42, 7)));

        // when
        WalletResponse wallet = scoreService.getMyWallet(1L);

        // then
        assertThat(wallet.totalScore()).isEqualTo(42);
        assertThat(wallet.pointBalance()).isEqualTo(7);
    }

    @Test
    void GLOBAL_랭킹_조회_상위목록_안에_있으면_그_순위를_쓴다() {
        // given
        given(globalRankingCache.isSeeded()).willReturn(true);
        given(globalRankingCache.getTop(anyInt())).willReturn(List.of(
                new ScoredEntry<>(300.0, 2L), new ScoredEntry<>(200.0, 1L), new ScoredEntry<>(100.0, 3L)
        ));
        given(userRepository.findAllById(any())).willReturn(List.of(
                userWithId(2L, "1등유저"), userWithId(1L, "나"), userWithId(3L, "3등유저")
        ));
        given(globalRankingCache.getRank(1L)).willReturn(Optional.of(1));

        // when
        RankingResponse ranking = scoreService.getRanking(1L, RankingScope.GLOBAL);

        // then
        assertThat(ranking.topEntries()).hasSize(3);
        assertThat(ranking.topEntries().get(1).userId()).isEqualTo(1L);
        assertThat(ranking.topEntries().get(1).rank()).isEqualTo(2);
        assertThat(ranking.myRank()).isEqualTo(2);
    }

    @Test
    void GLOBAL_랭킹_조회_상위목록_밖이면_별도로_순위를_계산한다() {
        // given
        given(globalRankingCache.isSeeded()).willReturn(true);
        given(globalRankingCache.getTop(anyInt())).willReturn(List.of());
        given(globalRankingCache.getRank(1L)).willReturn(Optional.empty());
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(userScoreWithId(1L, 5, 0)));
        given(userScoreRepository.countByTotalScoreGreaterThan(5)).willReturn(127L);

        // when
        RankingResponse ranking = scoreService.getRanking(1L, RankingScope.GLOBAL);

        // then
        assertThat(ranking.myRank()).isEqualTo(128);
    }

    @Test
    void GLOBAL_랭킹_콜드스타트면_DB에서_1회_backfill한다() {
        // given
        given(globalRankingCache.isSeeded()).willReturn(false);
        given(userScoreRepository.findAllByOrderByTotalScoreDesc()).willReturn(List.of(userScoreWithId(1L, 10, 0)));
        given(globalRankingCache.getTop(anyInt())).willReturn(List.of());
        given(globalRankingCache.getRank(1L)).willReturn(Optional.of(0));

        // when
        scoreService.getRanking(1L, RankingScope.GLOBAL);

        // then
        verify(globalRankingCache).backfill(eq(Map.of(1L, 10)));
    }

    @Test
    void NETWORK_랭킹은_내_촌수_안에서만_순위를_매긴다() {
        // given
        given(followRepository.findNetworkDegrees(eq(1L), anyInt(), anyInt())).willReturn(List.of(
                networkRow(2L, 1), networkRow(3L, 2)
        ));
        given(userScoreRepository.findByUserIdIn(any())).willReturn(List.of(
                userScoreWithId(1L, 50, 0), userScoreWithId(2L, 200, 0), userScoreWithId(3L, 10, 0)
        ));
        given(userRepository.findAllById(any())).willReturn(List.of(
                userWithId(1L, "나"), userWithId(2L, "1촌"), userWithId(3L, "2촌")
        ));
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(userScoreWithId(1L, 50, 0)));

        // when
        RankingResponse ranking = scoreService.getRanking(1L, RankingScope.NETWORK);

        // then — 전체 GLOBAL 데이터와 무관하게 네트워크(2L,3L,나) 안에서만 순위가 매겨진다
        assertThat(ranking.topEntries()).hasSize(3);
        assertThat(ranking.topEntries().get(0).userId()).isEqualTo(2L);
        assertThat(ranking.myRank()).isEqualTo(2);
    }

    @Test
    void NETWORK_랭킹은_네트워크가_비어있으면_자동_1등() {
        // given
        given(followRepository.findNetworkDegrees(eq(1L), anyInt(), anyInt())).willReturn(List.of());

        // when
        RankingResponse ranking = scoreService.getRanking(1L, RankingScope.NETWORK);

        // then
        assertThat(ranking.topEntries()).isEmpty();
        assertThat(ranking.myRank()).isEqualTo(1);
    }

    @Test
    void LOCAL_랭킹은_지역_리뷰수로_매겨지고_리뷰없으면_myRank가_null() {
        // given
        given(reviewRepository.countReviewsByRegion(eq("강남구"), anyInt())).willReturn(List.of(
                regionRow(2L, 5L), regionRow(3L, 2L)
        ));
        given(userRepository.findAllById(any())).willReturn(List.of(
                userWithId(2L, "동네고수"), userWithId(3L, "3등")
        ));
        given(reviewRepository.countMyReviewsByRegion(1L, "강남구")).willReturn(0L);

        // when
        LocalRankingResponse ranking = scoreService.getLocalRanking(1L, "강남구");

        // then
        assertThat(ranking.topEntries()).hasSize(2);
        assertThat(ranking.topEntries().get(0).nickname()).isEqualTo("동네고수");
        assertThat(ranking.myReviewCount()).isEqualTo(0);
        assertThat(ranking.myRank()).isNull();
    }

    @Test
    void LOCAL_랭킹은_내_리뷰가_있으면_내_순위를_계산한다() {
        // given
        given(reviewRepository.countReviewsByRegion(eq("강남구"), anyInt())).willReturn(List.of());
        given(reviewRepository.countMyReviewsByRegion(1L, "강남구")).willReturn(3L);
        given(reviewRepository.countUsersAheadInRegion("강남구", 3L)).willReturn(4L);

        // when
        LocalRankingResponse ranking = scoreService.getLocalRanking(1L, "강남구");

        // then
        assertThat(ranking.myReviewCount()).isEqualTo(3);
        assertThat(ranking.myRank()).isEqualTo(5);
    }
}
