# 앱이 런타임에 읽을 모든 비밀값을 하나의 JSON 시크릿으로 보관.
# DB호스트/비번/Redis호스트는 Terraform이 자동으로 채우고,
# JWT/카카오/네이버 값은 terraform.tfvars 로 주입한다.
resource "aws_secretsmanager_secret" "app" {
  name                    = "${local.name}/app"
  description             = "naknak app runtime secrets"
  recovery_window_in_days = 0 # 학습용: 삭제 시 즉시 제거
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    SPRING_DATASOURCE_URL      = "jdbc:postgresql://${aws_db_instance.main.address}:5432/naknak"
    SPRING_DATASOURCE_USERNAME = "naknak"
    SPRING_DATASOURCE_PASSWORD = random_password.db.result
    SPRING_DATA_REDIS_HOST     = aws_elasticache_cluster.redis.cache_nodes[0].address
    SPRING_DATA_REDIS_PORT     = "6379"
    JWT_SECRET                 = var.jwt_secret
    KAKAO_CLIENT_ID            = var.kakao_client_id
    KAKAO_CLIENT_SECRET        = var.kakao_client_secret
    KAKAO_REDIRECT_URI         = var.kakao_redirect_uri
    NAVER_CLIENT_ID            = var.naver_client_id
    NAVER_CLIENT_SECRET        = var.naver_client_secret
  })
}
