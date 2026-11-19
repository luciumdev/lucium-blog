terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  # state는 로컬 보관 (개인 프로젝트 규모)
  #  - terraform.tfstate 분실 시 리소스 추적 불가 → 별도 백업 필수
  #  - 이후 S3 backend(use_lockfile) 이전 가능
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
    }
  }
}

# CloudFront 커스텀 도메인용 ACM 인증서는 us-east-1 에서만 발급 가능
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"

  default_tags {
    tags = {
      Project   = var.project
      ManagedBy = "terraform"
    }
  }
}
