# 고정 공개 IP (EC2를 재생성해도 주소 유지)
resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id
  tags     = { Name = "${local.name}-eip" }
}

output "stable_ip" {
  value = aws_eip.app.public_ip
}

output "stable_url" {
  value = "http://${aws_eip.app.public_ip}:8080"
}
