# 우리 앱 Docker 이미지를 저장할 AWS 저장소
resource "aws_ecr_repository" "app" {
  name                 = "naknak-server"
  image_tag_mutability = "MUTABLE"
  force_delete         = true # 학습용: 이미지가 있어도 repo 삭제 허용

  image_scanning_configuration {
    scan_on_push = true # 푸시할 때 취약점 스캔
  }
}

output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}
