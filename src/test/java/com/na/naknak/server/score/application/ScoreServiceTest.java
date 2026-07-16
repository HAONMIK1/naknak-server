package com.na.naknak.server.score.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.score.domain.ScoreReason;
import com.na.naknak.server.score.domain.ScoreTarget;
import com.na.naknak.server.score.domain.UserScore;
import com.na.naknak.server.score.domain.UserScoreHistory;
import com.na.naknak.server.score.domain.repository.UserScoreHistoryRepository;
import com.na.naknak.server.score.domain.repository.UserScoreRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void 랭킹_조회_상위목록_안에_있으면_그_순위를_쓴다() {
        // given
        List<UserScore> top = List.of(
                userScoreWithId(2L, 300, 0),
                userScoreWithId(1L, 200, 0),
                userScoreWithId(3L, 100, 0)
        );
        given(userScoreRepository.findByOrderByTotalScoreDesc(any(Pageable.class))).willReturn(top);
        given(userRepository.findAllById(any())).willReturn(List.of(
                userWithId(2L, "1등유저"), userWithId(1L, "나"), userWithId(3L, "3등유저")
        ));
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(userScoreWithId(1L, 200, 0)));
        given(userScoreRepository.countByTotalScoreGreaterThan(200)).willReturn(1L);

        // when
        RankingResponse ranking = scoreService.getRanking(1L);

        // then
        assertThat(ranking.topEntries()).hasSize(3);
        assertThat(ranking.topEntries().get(1).userId()).isEqualTo(1L);
        assertThat(ranking.topEntries().get(1).rank()).isEqualTo(2);
        assertThat(ranking.myRank()).isEqualTo(2);
    }

    @Test
    void 랭킹_조회_상위목록_밖이면_별도로_순위를_계산한다() {
        // given
        given(userScoreRepository.findByOrderByTotalScoreDesc(any(Pageable.class))).willReturn(List.of());
        given(userScoreRepository.findByUserId(1L)).willReturn(Optional.of(userScoreWithId(1L, 5, 0)));
        given(userScoreRepository.countByTotalScoreGreaterThan(5)).willReturn(127L);

        // when
        RankingResponse ranking = scoreService.getRanking(1L);

        // then
        assertThat(ranking.myRank()).isEqualTo(128);
    }
}
