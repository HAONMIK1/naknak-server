resource "aws_elasticache_subnet_group" "main" {
  name       = "${local.name}-redis-subnet"
  subnet_ids = aws_subnet.private[*].id
}

# 단일 노드 -> 클러스터 모드(샤딩+복제)로 전환 (docs/scaling.md 3단계).
# 촌수 캐시/랭킹 ZSET/리프레시 토큰/블랙리스트가 전부 노드 1대에 몰려 있어 용량·처리량
# 한계와 단일 장애점(SPOF) 문제가 있었다 — 2개 샤드 + 샤드당 복제본 1개로 나눠서 용량을
# 수평으로 늘리고, 복제본이 있어 노드 하나가 죽어도 자동 페일오버되게 한다.
#
# ⚠️ terraform apply는 아직 실행하지 않았다. apply하는 시점에 애플리케이션의 Redis 연결
# 설정도 반드시 같이 바꿔야 한다 — 지금 코드(redisson-spring-boot-starter)는
# spring.data.redis.host/port(단일 서버) 설정을 쓰는데, 클러스터 모드 엔드포인트는 이
# 설정으로 정상 연결되지 않는다(프로토콜이 다름). apply와 동시에
# application.yml/secrets.tf를 spring.data.redis.cluster.nodes 기반으로 바꾸는 배포를
# 함께 진행해야 한다 — 순서가 어긋나면 앱이 Redis에 연결하지 못한다.
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id = "${local.name}-redis"
  description           = "naknak redis (cluster mode enabled)"
  engine                = "redis"
  engine_version        = "7.1"
  node_type             = "cache.t3.micro" # 노드 크기는 그대로, 개수만 늘림 (비용최적 유지)
  port                  = 6379

  parameter_group_name       = "default.redis7.cluster.on"
  num_node_groups            = 2 # 샤드 2개
  replicas_per_node_group    = 1 # 샤드당 복제본 1개 (자동 페일오버용)
  automatic_failover_enabled = true
  multi_az_enabled           = true

  subnet_group_name  = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]

  tags = { Name = "${local.name}-redis" }
}
