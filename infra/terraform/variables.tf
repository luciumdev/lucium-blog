variable "project" {
  description = "리소스 이름 접두사"
  type        = string
  default     = "lucium-blog"
}

variable "region" {
  description = "운영 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "instance_type" {
  description = "EC2 인스턴스 타입 (ES 힙 512m + JVM 384m 기준 2GB 필요)"
  type        = string
  default     = "t3.small"
}

variable "root_volume_size" {
  description = "루트 EBS 크기(GB) — Docker 이미지 + ES/PG 데이터"
  type        = number
  default     = 30
}

variable "uploads_bucket_name" {
  description = "업로드 S3 버킷 이름 (전 세계 유일해야 함)"
  type        = string
}

variable "cdn_domain" {
  description = "CloudFront 커스텀 도메인 (예: cdn.lucium.dev)"
  type        = string
  default     = "cdn.lucium.dev"
}

variable "upload_allowed_origins" {
  description = "브라우저 → S3 presigned PUT 을 허용할 Origin"
  type        = list(string)
  default     = ["https://lucium.dev", "https://www.lucium.dev"]
}

variable "github_repo" {
  description = "배포 권한을 줄 GitHub 저장소 (owner/repo)"
  type        = string

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repo))
    error_message = "github_repo 는 owner/repo 형식이어야 합니다."
  }
}

variable "github_deploy_branch" {
  description = "배포를 허용할 브랜치"
  type        = string
  default     = "main"
}

variable "budget_limit_usd" {
  description = "월 예산 한도(USD) — 80% 실제 / 100% 예측 초과 시 메일 알림"
  type        = number
  default     = 30
}

variable "budget_alert_email" {
  description = "예산 알림 수신 메일"
  type        = string
}
