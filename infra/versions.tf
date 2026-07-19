terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # 원격 상태 저장소 (Phase 0에서 미리 만든 S3 버킷/DynamoDB 이름으로 채웁니다)
  backend "s3" {
    bucket         = "naknak-tfstate-139214070114" # 예: naknak-tfstate-123456789012
    key            = "global/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "naknak-tflock"
    encrypt        = true
  }
}
