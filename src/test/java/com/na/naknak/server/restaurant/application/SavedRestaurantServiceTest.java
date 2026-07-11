package com.na.naknak.server.restaurant.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.SavedRestaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.restaurant.domain.repository.SavedRestaurantRepository;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import com.na.naknak.server.user.domain.User;
import com.na.naknak.server.user.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SavedRestaurantServiceTest {

    @InjectMocks
    private SavedRestaurantService savedRestaurantService;

    @Mock
    private SavedRestaurantRepository savedRestaurantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestaurantRepository restaurantRepository;

    private User userWithId(Long id) {
        User user = User.create(String.valueOf(id), "u" + id + "@test.com", "유저" + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Restaurant restaurantWithId(Long id) {
        Restaurant restaurant = Restaurant.create(null, null, "맛집" + id, "한식", "주소" + id, null, null);
        ReflectionTestUtils.setField(restaurant, "id", id);
        return restaurant;
    }

    @Test
    void 맛집_저장_성공() {
        // given
        given(savedRestaurantRepository.existsByUserIdAndRestaurantId(1L, 2L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(restaurantRepository.findById(2L)).willReturn(Optional.of(restaurantWithId(2L)));

        // when
        savedRestaurantService.save(1L, 2L);

        // then
        verify(savedRestaurantRepository).save(any(SavedRestaurant.class));
    }

    @Test
    void 이미_저장한_맛집_중복저장_안함() {
        // given
        given(savedRestaurantRepository.existsByUserIdAndRestaurantId(1L, 2L)).willReturn(true);

        // when
        savedRestaurantService.save(1L, 2L);

        // then
        verify(savedRestaurantRepository, never()).save(any(SavedRestaurant.class));
    }

    @Test
    void 존재하지_않는_맛집_저장_예외() {
        // given
        given(savedRestaurantRepository.existsByUserIdAndRestaurantId(1L, 999L)).willReturn(false);
        given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(1L)));
        given(restaurantRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> savedRestaurantService.save(1L, 999L))
                .isInstanceOf(BusinessException.class);
        verify(savedRestaurantRepository, never()).save(any(SavedRestaurant.class));
    }

    @Test
    void 저장_취소_성공() {
        // given
        SavedRestaurant saved = SavedRestaurant.create(userWithId(1L), restaurantWithId(2L));
        given(savedRestaurantRepository.findByUserIdAndRestaurantId(1L, 2L)).willReturn(Optional.of(saved));

        // when
        savedRestaurantService.unsave(1L, 2L);

        // then
        verify(savedRestaurantRepository).delete(saved);
    }

    @Test
    void 저장하지_않은_맛집_취소_시_무시() {
        // given
        given(savedRestaurantRepository.findByUserIdAndRestaurantId(1L, 2L)).willReturn(Optional.empty());

        // when
        savedRestaurantService.unsave(1L, 2L);

        // then
        verify(savedRestaurantRepository, never()).delete(any(SavedRestaurant.class));
    }

    @Test
    void 저장_목록_조회_페이징() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        SavedRestaurant saved = SavedRestaurant.create(userWithId(1L), restaurantWithId(2L));
        given(savedRestaurantRepository.findByUserId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(saved), pageable, 1));

        // when
        Page<RestaurantResponse> result = savedRestaurantService.getSaved(1L, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(2L);
    }
}
