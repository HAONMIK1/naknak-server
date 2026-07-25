package com.na.naknak.server.score.infrastructure.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.na.naknak.server.score.application.ScoreEarnEvent;
import com.na.naknak.server.score.application.ScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

/**
 * SQS를 롱폴링해서 점수 적립 이벤트를 처리하는 컨슈머. 처리에 성공한 메시지만 삭제(ack)한다 —
 * 실패한 메시지는 SQS visibility timeout이 지나면 자동으로 다시 보여서 재시도되고, 계속
 * 실패하면 DLQ로 이동한다(앱이 재시도 로직을 직접 구현하지 않음). 멱등성은 고려하지 않는다 —
 * at-least-once 전달을 전제로 하며, 중복 적립이 실제로 관측되면 그때 추가한다 (docs/score.md).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aws.sqs", name = "score-earn-queue-url")
public class ScoreEarnConsumer {

    private static final int MAX_MESSAGES_PER_POLL = 10;
    private static final int LONG_POLL_WAIT_SECONDS = 20;

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ScoreService scoreService;
    private final String queueUrl;

    public ScoreEarnConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            ScoreService scoreService,
            @Value("${aws.sqs.score-earn-queue-url}") String queueUrl
    ) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.scoreService = scoreService;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(MAX_MESSAGES_PER_POLL)
                .waitTimeSeconds(LONG_POLL_WAIT_SECONDS)
                .build()).messages();

        for (Message message : messages) {
            if (process(message)) {
                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build());
            }
        }
    }

    private boolean process(Message message) {
        try {
            ScoreEarnEvent event = objectMapper.readValue(message.body(), ScoreEarnEvent.class);
            scoreService.earn(event.userId(), event.reason(), event.reviewId());
            return true;
        } catch (Exception e) {
            log.error("점수 적립 이벤트 처리 실패 (messageId={})", message.messageId(), e);
            return false;
        }
    }
}
