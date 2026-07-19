# ---------- S3: 리뷰 사진 업로드 버킷 (비공개, CloudFront 만 접근 — front 버킷과 동일 패턴) ----------
resource "aws_s3_bucket" "uploads" {
  bucket        = "${local.name}-uploads-${data.aws_caller_identity.me.account_id}"
  force_destroy = true # 학습용: 파일 있어도 삭제 허용
}

resource "aws_s3_bucket_public_access_block" "uploads" {
  bucket                  = aws_s3_bucket.uploads.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "uploads" {
  name                              = "${local.name}-uploads-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# S3는 이 CloudFront 배포에서 오는 요청만 허용 (front 버킷과 동일)
resource "aws_s3_bucket_policy" "uploads" {
  bucket = aws_s3_bucket.uploads.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "cloudfront.amazonaws.com" }
      Action    = "s3:GetObject"
      Resource  = "${aws_s3_bucket.uploads.arn}/*"
      Condition = {
        StringEquals = { "AWS:SourceArn" = aws_cloudfront_distribution.front.arn }
      }
    }]
  })
}

# EC2가 이 버킷에만 업로드(PutObject)할 수 있게 하는 권한
resource "aws_iam_role_policy" "ec2_upload_s3" {
  name = "${local.name}-ec2-upload-s3"
  role = aws_iam_role.ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject"]
      Resource = "${aws_s3_bucket.uploads.arn}/*"
    }]
  })
}

output "uploads_bucket" {
  value = aws_s3_bucket.uploads.bucket
}
