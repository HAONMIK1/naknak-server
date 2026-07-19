# 요금이 조금이라도 발생하면 이메일 알림을 보낸다.
# (AWS는 자동 결제 차단 기능이 없으므로, 알림 + destroy 습관으로 관리한다.)
variable "budget_alert_email" {
  type        = string
  description = "요금 알림 받을 이메일"
  default     = "noa01094465840@gmail.com"
}

resource "aws_budgets_budget" "monthly" {
  name         = "${local.name}-monthly"
  budget_type  = "COST"
  limit_amount = "10"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # 실제 사용액이 $1 넘으면 즉시 알림 (거의 새자마자 알게 됨)
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 1
    threshold_type             = "ABSOLUTE_VALUE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.budget_alert_email]
  }

  # 이번 달 예상액이 $10(한도) 넘을 것 같으면 알림
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.budget_alert_email]
  }
}
