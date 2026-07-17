# 점수 적립 비동기 큐 (docs/score.md "점수 적립 비동기화" 참고).
# terraform apply는 아직 실행하지 않았다 — 실행 전까지는 애플리케이션의
# aws.sqs.score-earn-queue-url이 어떤 환경에도 설정되지 않으므로 동기 폴백
# (SyncScoreEarnPublisher)만 동작한다.

resource "aws_sqs_queue" "score_earn_dlq" {
  name                      = "${local.name}-score-earn-dlq"
  message_retention_seconds = 1209600 # 14일 — 원인 파악할 시간을 넉넉히 둔다
}

resource "aws_sqs_queue" "score_earn" {
  name                       = "${local.name}-score-earn"
  visibility_timeout_seconds = 30 # ScoreEarnConsumer 처리 시간(DB 쓰기 2~3건) 대비 여유
  message_retention_seconds  = 345600 # 4일

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.score_earn_dlq.arn
    maxReceiveCount      = 5 # 5번 실패하면 DLQ로 이동
  })
}

# EC2가 이 큐에만 메시지를 보내고(publisher) 받고 지울(consumer) 수 있게 하는 권한.
# 프로듀서/컨슈머가 같은 앱 프로세스 안에 있어서 권한을 분리하지 않는다.
resource "aws_iam_role_policy" "ec2_score_earn_queue" {
  name = "${local.name}-ec2-score-earn-queue"
  role = aws_iam_role.ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ]
      Resource = aws_sqs_queue.score_earn.arn
    }]
  })
}

output "score_earn_queue_url" {
  value = aws_sqs_queue.score_earn.url
}
