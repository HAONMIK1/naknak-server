provider "aws" {
  region = var.region

  # 모든 리소스에 공통 태그 자동 부착 (실무 표준)
  default_tags {
    tags = {
      Project     = var.project
      Environment = "prod"
      ManagedBy   = "terraform"
    }
  }
}
