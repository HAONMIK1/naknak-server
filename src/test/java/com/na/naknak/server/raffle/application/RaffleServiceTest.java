package com.na.naknak.server.raffle.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.raffle.domain.Raffle;
import com.na.naknak.server.raffle.domain.RaffleEntry;
import com.na.naknak.server.raffle.domain.RaffleStatus;
import com.na.naknak.server.raffle.domain.repository.RaffleEntryRepository;
import com.na.naknak.server.raffle.domain.repository.RaffleRepository;
import com.na.naknak.server.raffle.presentation.dto.MyEntryResponse;
import com.na.naknak.server.raffle.presentation.dto.RaffleDetailResponse;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.domain.ScoreReason;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RaffleServiceTest {

    @InjectMocks
    private RaffleService raffleService;

    @Mock
    private RaffleRepository raffleRepository;
    @Mock
    private RaffleEntryRepository raffleEntryRepository;
    @Mock
    private ScoreService scoreService;
    @Mock
    private UserRepository userRepository;

    private Raffle raffleWithId(Long id, LocalDateTime revealAt) {
        Raffle raffle = Raffle.create("여름 이벤트", "교촌치킨 상품권", 20000, 100, revealAt);
        ReflectionTestUtils.setField(raffle, "id", id);
        return raffle;
    }

    private User userWithId(Long id, String nickname) {
        User user = User.create(String.valueOf(id), "u" + id + "@test.com", nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 응모하면_포인트가_차감되고_엔트리가_생긴다() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().plusDays(1));
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleIdAndUserId(1L, 10L)).willReturn(Optional.empty());
        given(raffleEntryRepository.save(any(RaffleEntry.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        MyEntryResponse response = raffleService.enter(10L, 1L);

        // then
        verify(scoreService).spendPoints(10L, 100, ScoreReason.RAFFLE_ENTRY);
        assertThat(response.myEntryCount()).isEqualTo(1);
    }

    @Test
    void 이미_응모한_적_있으면_엔트리가_누적된다() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().plusDays(1));
        RaffleEntry existing = RaffleEntry.create(1L, 10L);
        existing.addEntry();
        existing.addEntry();
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleIdAndUserId(1L, 10L)).willReturn(Optional.of(existing));

        // when
        MyEntryResponse response = raffleService.enter(10L, 1L);

        // then
        assertThat(response.myEntryCount()).isEqualTo(3);
    }

    @Test
    void 존재하지_않는_래플_응모시_예외() {
        // given
        given(raffleRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> raffleService.enter(10L, 999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 공개일이_지난_래플에_응모하면_지연추첨후_마감_예외() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().minusMinutes(1));
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleId(1L)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> raffleService.enter(10L, 1L))
                .isInstanceOf(BusinessException.class);
        assertThat(raffle.getStatus()).isEqualTo(RaffleStatus.DRAWN);
        verify(scoreService, never()).spendPoints(any(), anyInt(), any());
    }

    @Test
    void 공개일이_지나면_조회만으로도_추첨되어_참여자중_당첨된다() {
        // given
        Raffle raffle = raffleWithId(5L, LocalDateTime.now().minusMinutes(1));
        RaffleEntry onlyEntrant = RaffleEntry.create(5L, 7L);
        onlyEntrant.addEntry();
        onlyEntrant.addEntry();
        onlyEntrant.addEntry();
        given(raffleRepository.findById(5L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleId(5L)).willReturn(List.of(onlyEntrant));
        given(raffleEntryRepository.findByRaffleIdAndUserId(5L, 7L)).willReturn(Optional.of(onlyEntrant));
        given(userRepository.findById(7L)).willReturn(Optional.of(userWithId(7L, "당첨자")));

        // when
        RaffleDetailResponse response = raffleService.getDetail(7L, 5L);

        // then
        assertThat(raffle.getStatus()).isEqualTo(RaffleStatus.DRAWN);
        assertThat(raffle.getWinnerUserId()).isEqualTo(7L);
        assertThat(response.isWinner()).isTrue();
        assertThat(response.winnerNickname()).isEqualTo("당첨자");
    }

    @Test
    void 참여자가_없으면_유찰된다() {
        // given
        Raffle raffle = raffleWithId(6L, LocalDateTime.now().minusMinutes(1));
        given(raffleRepository.findById(6L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleId(6L)).willReturn(List.of());
        given(raffleEntryRepository.findByRaffleIdAndUserId(6L, 10L)).willReturn(Optional.empty());

        // when
        RaffleDetailResponse response = raffleService.getDetail(10L, 6L);

        // then
        assertThat(raffle.getStatus()).isEqualTo(RaffleStatus.DRAWN);
        assertThat(raffle.getWinnerUserId()).isNull();
        assertThat(response.winnerNickname()).isNull();
        assertThat(response.isWinner()).isFalse();
    }

    @Test
    void 이미_추첨된_래플은_다시_추첨하지_않는다() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().minusDays(1));
        raffle.draw(7L);
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleIdAndUserId(1L, 10L)).willReturn(Optional.empty());

        // when
        raffleService.getDetail(10L, 1L);

        // then
        verify(raffleEntryRepository, never()).findByRaffleId(any());
    }

    @Test
    void 당첨자가_아니면_상품권_코드를_볼_수_없다() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().minusDays(1));
        raffle.draw(7L);
        ReflectionTestUtils.setField(raffle, "giftCode", "GIFT-CODE-123");
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleIdAndUserId(1L, 99L)).willReturn(Optional.empty());
        given(userRepository.findById(7L)).willReturn(Optional.of(userWithId(7L, "당첨자")));

        // when
        RaffleDetailResponse response = raffleService.getDetail(99L, 1L);

        // then
        assertThat(response.giftCode()).isNull();
        assertThat(response.winnerNickname()).isEqualTo("당첨자");
    }

    @Test
    void 당첨자_본인은_상품권_코드를_볼_수_있다() {
        // given
        Raffle raffle = raffleWithId(1L, LocalDateTime.now().minusDays(1));
        raffle.draw(7L);
        ReflectionTestUtils.setField(raffle, "giftCode", "GIFT-CODE-123");
        given(raffleRepository.findById(1L)).willReturn(Optional.of(raffle));
        given(raffleEntryRepository.findByRaffleIdAndUserId(1L, 7L)).willReturn(Optional.empty());
        given(userRepository.findById(7L)).willReturn(Optional.of(userWithId(7L, "당첨자")));

        // when
        RaffleDetailResponse response = raffleService.getDetail(7L, 1L);

        // then
        assertThat(response.giftCode()).isEqualTo("GIFT-CODE-123");
    }
}
