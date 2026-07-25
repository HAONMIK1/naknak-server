package com.na.naknak.server.restaurant.application;

import com.na.naknak.server.common.exception.BusinessException;
import com.na.naknak.server.restaurant.domain.Restaurant;
import com.na.naknak.server.restaurant.domain.repository.RestaurantRepository;
import com.na.naknak.server.restaurant.infrastructure.naver.NaverSearchClient;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantRegisterRequest;
import com.na.naknak.server.restaurant.presentation.dto.RestaurantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class RestaurantIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RestaurantService restaurantService;
    @Autowired
    private RestaurantRepository restaurantRepository;

    @MockitoBean
    private NaverSearchClient naverSearchClient;

    @Test
    void 맛집_등록_신규_DB저장() {
        RestaurantRegisterRequest request = new RestaurantRegisterRequest(
                "스시로", "일식", "서울 강남구 테헤란로 1", 37.5, 127.0, "http://place");

        RestaurantResponse response = restaurantService.register(request);

        assertThat(response.id()).isNotNull();
        assertThat(restaurantRepository.findById(response.id())).isPresent();
        assertThat(restaurantRepository.findByNameAndAddress("스시로", "서울 강남구 테헤란로 1")).isPresent();
    }

    @Test
    void 맛집_등록_중복시_기존_재사용() {
        RestaurantRegisterRequest request = new RestaurantRegisterRequest(
                "스시로", "일식", "서울 강남구 테헤란로 1", 37.5, 127.0, "http://place");

        RestaurantResponse first = restaurantService.register(request);
        RestaurantResponse second = restaurantService.register(request);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(restaurantRepository.count()).isEqualTo(1);
    }

    @Test
    void 등록된_맛집_이름검색() {
        restaurantRepository.save(Restaurant.create(
                null, null, "스시로", "일식", "서울 강남구", 37.5, 127.0));
        restaurantRepository.save(Restaurant.create(
                null, null, "스시집", "일식", "서울 마포구", 37.5, 126.9));
        restaurantRepository.save(Restaurant.create(
                null, null, "국밥천국", "한식", "서울 종로구", 37.5, 127.0));

        Page<RestaurantResponse> result = restaurantService.search("스시", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(RestaurantResponse::name)
                .containsExactlyInAnyOrder("스시로", "스시집");
    }

    @Test
    void 상세조회_없으면_예외() {
        assertThatThrownBy(() -> restaurantService.getDetail(999999L))
                .isInstanceOf(BusinessException.class);
    }
}