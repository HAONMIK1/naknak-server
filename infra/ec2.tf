# SSH 접속용 키 (내 PC의 공개키를 등록)
variable "ssh_public_key" {
  type        = string
  description = "SSH 공개키 내용 (~/.ssh/naknak.pub)"
}

resource "aws_key_pair" "main" {
  key_name   = "${local.name}-key"
  public_key = var.ssh_public_key
}

# 최신 Amazon Linux 2023 AMI 자동 조회
data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
}

# 앱을 실행할 EC2 (외부 22/8080 허용)
resource "aws_security_group" "ec2_public" {
  name        = "${local.name}-ec2-public-sg"
  description = "SSH and app port from internet"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "app"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = { Name = "${local.name}-ec2-public-sg" }
}

resource "aws_instance" "app" {
  ami           = data.aws_ami.al2023.id
  instance_type = "t3.micro" # 프리티어

  subnet_id                   = aws_subnet.public[0].id
  associate_public_ip_address = true

  # app_sg: RDS/Redis 접근용(그 SG들이 app_sg 출처를 허용함)
  # ec2_public: 외부에서 22/8080 접근용
  vpc_security_group_ids = [aws_security_group.app.id, aws_security_group.ec2_public.id]

  iam_instance_profile = aws_iam_instance_profile.ec2.name
  key_name             = aws_key_pair.main.key_name

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    region    = var.region
    ecr_url   = aws_ecr_repository.app.repository_url
    secret_id = aws_secretsmanager_secret.app.name
  })
  user_data_replace_on_change = true

  tags = { Name = "${local.name}-app" }
}

output "app_public_ip" {
  value = aws_instance.app.public_ip
}

output "app_url" {
  value = "http://${aws_instance.app.public_ip}:8080"
}
