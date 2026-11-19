output "account_id" {
  value = data.aws_caller_identity.current.account_id
}

# Gabia DNS: lucium.dev / www.lucium.dev A 레코드 값
output "elastic_ip" {
  value = aws_eip.app.public_ip
}

# GitHub Variables: EC2_INSTANCE_ID
output "instance_id" {
  value = aws_instance.app.id
}

# GitHub Secrets: AWS_DEPLOY_ROLE_ARN
output "ci_deploy_role_arn" {
  value = aws_iam_role.ci_deploy.arn
}

# 서버 .env: AWS_S3_BUCKET
output "uploads_bucket" {
  value = aws_s3_bucket.uploads.bucket
}

# Gabia DNS: cdn CNAME 값
output "cloudfront_domain" {
  value = aws_cloudfront_distribution.cdn.domain_name
}

# 서버 .env: AWS_CLOUDFRONT_BASE_URL (끝 슬래시 없음 — S3Service.extractKey 기준)
output "cdn_base_url" {
  value = "https://${var.cdn_domain}"
}

# Gabia DNS: ACM 검증 CNAME (1단계 apply 후 확인)
output "acm_validation_records" {
  value = [
    for o in aws_acm_certificate.cdn.domain_validation_options : {
      name  = o.resource_record_name
      type  = o.resource_record_type
      value = o.resource_record_value
    }
  ]
}
