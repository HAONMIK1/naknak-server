package com.na.naknak.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 프로덕션과 동일한 경로(Flyway V1 스키마 적용 + Hibernate ddl-auto=validate)로 컨텍스트를 부팅한다.
 * 엔티티 매핑이 실제 마이그레이션 스키마와 불일치하면 이 테스트가 실패한다.
 * (일반 테스트 프로파일은 create-drop 이라 이 검증을 하지 못한다.)
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SchemaValidationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Test
	void 엔티티가_Flyway_스키마와_일치한다() {
	}
}
