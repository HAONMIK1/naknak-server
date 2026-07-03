package com.na.naknak.server.restaurant.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.restaurant.infrastructure.naver.NaverSearchClient;
import com.na.naknak.server.restaurant.presentation.dto.NaverPlaceResponse;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantRegisterRequest;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class RestaurantServiceTest {

    @InjectMocks
    private RestaurantService restaurantService;

    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private NaverSearchClient naverSearchClient;

    @Test
    void 네이버_검색_위임() {
        // given
        List<NaverPlaceResponse> places = List.of(
                new NaverPlaceResponse("강남파스타", "이탈리아음식", "서울 강남구", "서울 강남대로", 37.5, 127.0, "http://url")
        );
        given(naverSearchClient.search("강남 파스타")).willReturn(places);

        // when
        List<NaverPlaceResponse> result = restaurantService.searchNaver("강남 파스타");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("강남파스타");
    }

    @Test
    void 빈_키워드_네이버검색_예외() {
        assertThatThrownBy(() -> restaurantService.searchNaver(" "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 맛집_등록_신규_저장() {
        // given
        RestaurantRegisterRequest request = new RestaurantRegisterRequest(
                "새맛집", "한식", "서울 강남구 역삼동", 37.5, 127.0, "http://place");
        given(restaurantRepository.findByNameAndAddress("새맛집", "서울 강남구 역삼동"))
                .willReturn(Optional.empty());
        given(restaurantRepository.save(any(Restaurant.class)))
                .willAnswer(inv -> inv.getArgument(0));

        // when
        RestaurantResponse response = restaurantService.register(request);

        // then
        assertThat(response.name()).isEqualTo("새맛집");
        assertThat(response.address()).isEqualTo("서울 강남구 역삼동");
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    void 맛집_등록_중복시_기존_반환하고_저장_안함() {
        // given
        RestaurantRegisterRequest request = new RestaurantRegisterRequest(
                "중복맛집", "한식", "서울 강남구", null, null, null);
        Restaurant existing = Restaurant.create(null, null, "중복맛집", "한식", "서울 강남구", null, null);
        ReflectionTestUtils.setField(existing, "id", 10L);
        given(restaurantRepository.findByNameAndAddress("중복맛집", "서울 강남구"))
                .willReturn(Optional.of(existing));

        // when
        RestaurantResponse response = restaurantService.register(request);

        // then
        assertThat(response.id()).isEqualTo(10L);
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void 상세조회_없으면_예외() {
        given(restaurantRepository.findById(999L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> restaurantService.getDetail(999L))
                .isInstanceOf(BusinessException.class);
    }
}