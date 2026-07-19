# ---------- S3: 프론트 정적 파일 버킷 (비공개, CloudFront 만 접근) ----------
resource "aws_s3_bucket" "front" {
  bucket        = "${local.name}-front-${data.aws_caller_identity.me.account_id}"
  force_destroy = true # 학습용: 파일 있어도 삭제 허용
}

data "aws_caller_identity" "me" {}

resource "aws_s3_bucket_public_access_block" "front" {
  bucket                  = aws_s3_bucket.front.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# CloudFront 가 S3 에 접근할 수 있게 하는 인증 (OAC, 실무 표준)
resource "aws_cloudfront_origin_access_control" "front" {
  name                              = "${local.name}-front-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# ---------- CloudFront: HTTPS CDN. / -> S3, /api/* -> EC2 ----------
resource "aws_cloudfront_distribution" "front" {
  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_200" # 아시아 포함
  comment             = "${local.name} frontend + api proxy"

  # 오리진 1: S3 (정적 프론트)
  origin {
    domain_name              = aws_s3_bucket.front.bucket_regional_domain_name
    origin_id                = "s3-front"
    origin_access_control_id = aws_cloudfront_origin_access_control.front.id
  }

  # 오리진 2: EC2 백엔드 (HTTP:8080)
  origin {
    domain_name = aws_eip.app.public_dns
    origin_id   = "ec2-api"
    custom_origin_config {
      http_port              = 8080
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # 오리진 3: S3 (리뷰 사진 업로드)
  origin {
    domain_name              = aws_s3_bucket.uploads.bucket_regional_domain_name
    origin_id                = "s3-uploads"
    origin_access_control_id = aws_cloudfront_origin_access_control.uploads.id
  }

  # 기본: 정적 파일 (캐시 O)
  default_cache_behavior {
    target_origin_id       = "s3-front"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6" # Managed-CachingOptimized
  }

  # /api/* 는 EC2 로 프록시 (캐시 X, 모든 메서드/헤더 전달)
  ordered_cache_behavior {
    path_pattern             = "/api/*"
    target_origin_id         = "ec2-api"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = "4135ea2d-6df8-44a3-9df3-4b5a84be39ad" # Managed-CachingDisabled
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3" # Managed-AllViewer
  }

  # /uploads/* 는 S3 사진 (캐시 O — 업로드 후 내용이 안 바뀌는 정적 파일)
  ordered_cache_behavior {
    path_pattern           = "/uploads/*"
    target_origin_id       = "s3-uploads"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD"]
    cached_methods         = ["GET", "HEAD"]
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6" # Managed-CachingOptimized
  }

  # SPA 라우팅: 없는 경로는 index.html 로 (wouter 클라이언트 라우팅)
  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }
  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true # 기본 *.cloudfront.net 인증서 (도메인 사면 ACM 으로 교체)
  }
}

# S3 는 이 CloudFront 배포에서 오는 요청만 허용
resource "aws_s3_bucket_policy" "front" {
  bucket = aws_s3_bucket.front.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.front.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.front.arn }
      }
    }]
  })
}

output "front_bucket" {
  value = aws_s3_bucket.front.id
}

output "cdn_domain" {
  value = "https://${aws_cloudfront_distribution.front.domain_name}"
}
