package com.na.naknak.server.score.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.score.application.ScoreEarnPublisher;
import com.na.naknak.server.score.application.ScoreService;
import com.na.naknak.server.score.infrastructure.sqs.SqsScoreEarnPublisher;
import com.na.naknak.server.score.infrastructure.sync.SyncScoreEarnPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * ScoreEarnPublisher 구현체를 딱 하나만 등록한다 — SQS 큐가 설정된 환경(운영)에서는 SQS로,
 * 아니면(로컬/테스트) 동기 직접호출로. 두 @Bean 메서드를 같은 @Configuration 클래스 안에 둬야
 * @ConditionalOnMissingBean의 평가 순서가 보장된다(플레인 @Component 두 개로 나눠두면 컴포넌트
 * 스캔 순서에 따라 둘 다 등록되지 않을 수 있음 — 실제로 그렇게 겪었다).
 */
@Configuration
public class ScoreEarnPublisherConfig {

    @Bean
    @ConditionalOnProperty(prefix = "aws.sqs", name = "score-earn-queue-url")
    public ScoreEarnPublisher sqsScoreEarnPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.score-earn-queue-url}") String queueUrl
    ) {
        return new SqsScoreEarnPublisher(sqsClient, objectMapper, queueUrl);
    }

    @Bean
    @ConditionalOnMissingBean(ScoreEarnPublisher.class)
    public ScoreEarnPublisher syncScoreEarnPublisher(ScoreService scoreService) {
        return new SyncScoreEarnPublisher(scoreService);
    }
}
