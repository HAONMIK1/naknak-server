# 앱 비밀값 (terraform.tfvars 에 실제 값을 넣는다 — 이 파일엔 값 없음)
variable "jwt_secret" {
  type      = string
  sensitive = true
}

variable "kakao_client_id" {
  type      = string
  sensitive = true
}

variable "kakao_client_secret" {
  type      = string
  sensitive = true
}

# ALB 주소가 생기는 Phase 4 이후 실제 값으로 채운다. 지금은 비워둬도 됨.
variable "kakao_redirect_uri" {
  type    = string
  default = ""
}

variable "naver_client_id" {
  type      = string
  sensitive = true
}

variable "naver_client_secret" {
  type      = string
  sensitive = true
}
