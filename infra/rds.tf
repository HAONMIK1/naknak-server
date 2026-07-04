# DB 마스터 비밀번호는 사람이 정하지 않고 자동 생성 → Secrets Manager에만 보관
resource "random_password" "db" {
  length           = 20
  special          = true
  override_special = "!#$%^&*()-_=+"
}

# RDS를 어느 서브넷(프라이빗)에 둘지 그룹으로 지정
resource "aws_db_subnet_group" "main" {
  name       = "${local.name}-db-subnet"
  subnet_ids = aws_subnet.private[*].id
  tags       = { Name = "${local.name}-db-subnet" }
}

resource "aws_db_instance" "main" {
  identifier     = "${local.name}-db"
  engine         = "postgres"
  engine_version = "16.14"
  instance_class = "db.t3.micro" # 프리티어 대상

  allocated_storage = 20
  storage_type      = "gp3"

  db_name  = "naknak"
  username = "naknak"
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az            = false # 비용최적: 단일 AZ
  publicly_accessible = false # 외부에서 직접 접근 불가

  skip_final_snapshot = true  # 학습용: 삭제 시 스냅샷 안 남김
  deletion_protection = false # 학습용: 삭제 허용
  apply_immediately   = true

  tags = { Name = "${local.name}-db" }
}
