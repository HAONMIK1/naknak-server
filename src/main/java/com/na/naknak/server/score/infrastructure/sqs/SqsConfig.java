package com.na.naknak.server.score.infrastructure.sqs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * aws.sqs.score-earn-queue-url이 설정된 환경(운영)에서만 SqsClient 빈을 만든다 — 로컬/테스트는
 * SQS 자체를 몰라도 되게(SyncScoreEarnPublisher 폴백) 하기 위함.
 */
@Configuration
@ConditionalOnProperty(prefix = "aws.sqs", name = "score-earn-queue-url")
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region}") String region) {
        return SqsClient.builder().region(Region.of(region)).build();
    }
}
