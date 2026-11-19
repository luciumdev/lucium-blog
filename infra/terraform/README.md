# lucium.dev 인프라 (Terraform)

## 관리 범위

| Terraform 관리 | 수동 관리 |
|---|---|
| EC2 t3.small · EIP · 보안 그룹(80/443) | 루트 MFA · 관리자 IAM 사용자 |
| 인스턴스 Role (SSM + 업로드 버킷 쓰기/삭제) | Gabia DNS 레코드 |
| S3 업로드 버킷 · CORS · CloudFront(OAC) | 서버 `.env` · Docker Hub 로그인 |
| ACM(us-east-1) `cdn.lucium.dev` | certbot 최초 발급 |
| GitHub OIDC Provider · CI 배포 Role | GitHub Secrets / Variables |
| 월 예산 알림 | |

- 액세스 키 0개 운영
  - 앱 → 인스턴스 Role (IMDSv2, hop limit 2)
  - CI → GitHub OIDC
  - 사람 → 관리자 사용자 + MFA
- state 는 로컬 보관 → `terraform.tfstate` 분실 시 리소스 추적 불가, 별도 백업 필수

---

## 0. 계정 준비 (콘솔, 1회)

1. 루트 계정 MFA 등록
2. IAM 사용자 `lucium-admin` 생성
   - `AdministratorAccess` 연결, MFA 등록
   - 액세스 키 발급 → 로컬 `aws configure --profile lucium`
3. 이후 루트 계정 사용 중단

## 1. 변수 파일

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # 값 채우기
export AWS_PROFILE=lucium
terraform init
```

- `uploads_bucket_name`: 전 세계 유일 이름 (이전 계정 버킷과 중복 불가)
- `github_repo`: `owner/repo` 정확히 — OIDC 신뢰 조건에 그대로 사용됨

## 2. ACM 인증서 먼저 생성 → Gabia 검증 레코드 등록

```bash
terraform apply -target=aws_acm_certificate.cdn
terraform output acm_validation_records
```

- output 이 비어 있으면 `terraform state show aws_acm_certificate.cdn` 의 `domain_validation_options` 확인
- Gabia DNS 에 CNAME 추가
  - 호스트: `name` 에서 `.lucium.dev.` 제거한 앞부분 (예: `_abc123.cdn`)
  - 값: `value` 그대로 (예: `_xyz.acm-validations.aws.`)

## 3. 전체 생성

```bash
terraform plan
terraform apply
```

- ACM 검증 완료까지 대기 (DNS 전파 수 분 ~ 수십 분, 최대 2시간 타임아웃)
- CloudFront 생성 5~10분 소요

```bash
terraform output
```

## 4. Gabia DNS

| 호스트 | 타입 | 값 |
|---|---|---|
| `@` | A | `elastic_ip` |
| `www` | A | `elastic_ip` |
| `cdn` | CNAME | `cloudfront_domain` (끝에 `.` 필요 여부는 Gabia 입력 규칙 따름) |

- `www` 레코드 없으면 certbot 발급 실패 (`-d www.lucium.dev` 포함)
- 확인: `nslookup lucium.dev` / `nslookup www.lucium.dev` → EIP

## 5. GitHub 설정

- Secrets
  - 추가: `AWS_DEPLOY_ROLE_ARN` = `ci_deploy_role_arn`
  - 유지: `DOCKER_USERNAME`, `DOCKER_PASSWORD`
  - 삭제: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`
- Variables
  - 추가: `EC2_INSTANCE_ID` = `instance_id`

## 6. 서버 세팅 (SSM 세션)

```bash
aws ssm start-session --target <instance_id>
# 또는 콘솔 EC2 → 연결 → Session Manager

sudo -iu ubuntu
cloud-init status --wait          # user_data(도커·swap) 완료 확인 → status: done
docker version && free -h
```

### 6-1. 파일 배치 (`/home/ubuntu/blog`)

- 저장소에서 복사, 내용은 heredoc 붙여넣기 (`cat > 파일 <<'EOF' ... EOF`)
  - `compose.yaml`
  - `nginx/nginx.conf` (1차 HTTP)
  - `nginx/nginx-https.conf` (2차 HTTPS)
  - `elasticsearch/Dockerfile`

### 6-2. `.env`

```bash
cd ~/blog
nano .env && chmod 600 .env
```

```dotenv
DB_URL=jdbc:postgresql://db:5432/blog
DB_USERNAME=blog
DB_PASSWORD=<랜덤>
REDIS_HOST=redis
REDIS_PASSWORD=<랜덤>
ELASTICSEARCH_URI=http://elasticsearch:9200
JWT_SECRET=<32자 이상 랜덤>
CORS_ALLOWED_ORIGINS=https://lucium.dev,https://www.lucium.dev
AWS_S3_BUCKET=<uploads_bucket>
AWS_CLOUDFRONT_BASE_URL=https://cdn.lucium.dev
KOREAEXIM_API_KEY=
KIS_APP_KEY=
KIS_APP_SECRET=
GOLDAPI_API_KEY=
ALPHAVANTAGE_API_KEY=
CLAUDE_API_KEY=
ADMIN_SEED_EMAIL=
ADMIN_SEED_PASSWORD=
```

- `DB_URL` DB 이름 = compose `POSTGRES_DB: blog`
- `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` 넣지 않음 → 인스턴스 Role 사용
- `AWS_CLOUDFRONT_BASE_URL` 끝 슬래시 금지 (S3Service 키 추출 기준)
- 랜덤 값 생성: `openssl rand -base64 36`

### 6-3. Docker Hub 로그인 (blog-api private 이미지)

```bash
docker login -u <DOCKER_USERNAME>   # 액세스 토큰 입력
```

## 7. 기동 + HTTPS

```bash
cd ~/blog
docker compose up -d                 # nginx.conf(HTTP) 상태로 전체 기동
docker compose ps                    # app healthy 확인 (start_period 60s)
curl -I http://lucium.dev            # 200

# 인증서 최초 발급
docker compose run --rm --entrypoint certbot certbot certonly \
  --webroot -w /var/www/certbot \
  -d lucium.dev -d www.lucium.dev \
  --email <메일> --agree-tos --no-eff-email

# HTTPS 설정으로 교체
cp nginx/nginx-https.conf nginx/nginx.conf
docker compose restart web
curl -I https://lucium.dev           # 200
```

### 인증서 갱신 반영

- certbot 컨테이너가 12시간마다 갱신하지만 nginx 는 재시작 전까지 이전 인증서 유지
- 배포가 뜸하면 만료 가능 → 주 1회 reload 등록

```bash
crontab -e
# 매주 월 04:00
0 4 * * 1 cd /home/ubuntu/blog && docker compose exec -T web nginx -s reload >/dev/null 2>&1
```

## 8. 첫 배포 · 확인

- `main` push → Actions `build-and-deploy` 통과 확인
- 체크리스트
  - [ ] `https://lucium.dev` 접속 · API 응답
  - [ ] 관리자 로그인 (AdminSeeder)
  - [ ] 글 작성 중 이미지 업로드 → `https://cdn.lucium.dev/uploads/...` 표시
    - 403 (PUT): S3 CORS `upload_allowed_origins` 확인
    - 500 (presigned 발급): 앱 로그에서 자격증명 오류 확인 → IMDS hop limit
  - [ ] 이미지 삭제 동작
  - [ ] 검색 (ES 기동 · nori)
  - [ ] 예산 알림 메일 구독 확인 (AWS Budgets 메일)

## 변경 · 폐기

- `user_data.sh` 수정은 기존 인스턴스에 반영 안 됨 (`ignore_changes`) → 서버에서 직접 적용
- AMI 갱신도 무시됨 → 인스턴스 교체가 필요하면 데이터 백업 후 `terraform apply -replace=aws_instance.app`
- `terraform destroy` 시 버킷에 객체가 있으면 실패 (의도된 보호)
