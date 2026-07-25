package com.na.naknak.server.score.infrastructure.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.score.application.ScoreEarnEvent;
import com.na.naknak.server.score.application.ScoreEarnPublisher;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * 점수 적립 요청을 SQS 큐에 던지기만 한다 — 실제 적립 처리는 {@link ScoreEarnConsumer}가 한다.
 * 빈 등록은 {@link ScoreEarnPublisherConfig}가 담당한다.
 */
@Slf4j
public class SqsScoreEarnPublisher implements ScoreEarnPublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    public SqsScoreEarnPublisher(SqsClient sqsClient, ObjectMapper objectMapper, String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(ScoreEarnEvent event) {
        try {
            String body = objectMapper.writeValueAsString(event);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        } catch (Exception e) {
            log.error("점수 적립 이벤트 발행 실패 (userId={}, reason={})", event.userId(), event.reason(), e);
        }
    }
}
