# EC2가 ECR에서 이미지 받고, Secrets Manager에서 비밀값 읽을 수 있게 하는 권한
resource "aws_iam_role" "ec2" {
  name = "${local.name}-ec2-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

# ECR 읽기(이미지 pull) - AWS 관리형 정책
resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

# 우리 시크릿만 읽기 허용
resource "aws_iam_role_policy" "read_secret" {
  name = "${local.name}-read-secret"
  role = aws_iam_role.ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["secretsmanager:GetSecretValue"]
      Resource = aws_secretsmanager_secret.app.arn
    }]
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.name}-ec2-profile"
  role = aws_iam_role.ec2.name
}
