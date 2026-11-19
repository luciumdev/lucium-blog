# ── 네트워크: 기본 VPC 사용 (NAT·전용 VPC 비용 없음) ─────────────
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
  filter {
    name   = "default-for-az"
    values = ["true"]
  }
}

# Ubuntu 24.04 LTS (amazon-ssm-agent 기본 내장)
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd-gp3/ubuntu-noble-24.04-amd64-server-*"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ── 보안 그룹: 80/443 만 개방, 22 차단 (접속은 SSM 전용) ──────────
resource "aws_security_group" "web" {
  name        = "${var.project}-web"
  description = "HTTP/HTTPS only. SSH closed, access via SSM."
  vpc_id      = data.aws_vpc.default.id
}

resource "aws_vpc_security_group_ingress_rule" "http" {
  security_group_id = aws_security_group.web.id
  description       = "HTTP (ACME challenge + HTTPS redirect)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "https" {
  security_group_id = aws_security_group.web.id
  description       = "HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

# 아웃바운드 전체 허용 (외부 API·Docker Hub·SSM 엔드포인트)
resource "aws_vpc_security_group_egress_rule" "all" {
  security_group_id = aws_security_group.web.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# ── 인스턴스 Role: SSM 접속 + 업로드 버킷 쓰기/삭제 ───────────────
# 앱은 S3Config 의 DefaultCredentialsProvider 로 이 Role 자격증명을 사용
# → .env 에 AWS 액세스 키 불필요
data "aws_iam_policy_document" "ec2_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "app" {
  name               = "${var.project}-app"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume.json
}

resource "aws_iam_role_policy_attachment" "app_ssm" {
  role       = aws_iam_role.app.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# S3Service: presigned PUT 발급(PutObject) + 삭제(DeleteObject), 키 접두사 uploads/
data "aws_iam_policy_document" "app_s3" {
  statement {
    actions   = ["s3:PutObject", "s3:DeleteObject"]
    resources = ["${aws_s3_bucket.uploads.arn}/uploads/*"]
  }
}

resource "aws_iam_role_policy" "app_s3" {
  name   = "uploads-write"
  role   = aws_iam_role.app.id
  policy = data.aws_iam_policy_document.app_s3.json
}

resource "aws_iam_instance_profile" "app" {
  name = "${var.project}-app"
  role = aws_iam_role.app.name
}

# ── EC2 ───────────────────────────────────────────────────────────
resource "aws_instance" "app" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = sort(data.aws_subnets.default.ids)[0]
  vpc_security_group_ids = [aws_security_group.web.id]
  iam_instance_profile   = aws_iam_instance_profile.app.name
  user_data              = file("${path.module}/user_data.sh")

  metadata_options {
    http_tokens   = "required" # IMDSv2 강제
    http_endpoint = "enabled"
    # Docker 컨테이너(app)에서 IMDS 로 Role 자격증명을 받으려면 2 이상 필요
    # 1이면 컨테이너 내부 요청이 브리지 홉에서 버려져 S3 호출이 자격증명 오류로 실패
    http_put_response_hop_limit = 2
  }

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    encrypted             = true
    delete_on_termination = true
  }

  tags = {
    Name = var.project
  }

  lifecycle {
    # 새 AMI 출시·user_data 수정 시 인스턴스 교체(=데이터 유실) 방지
    ignore_changes = [ami, user_data]
  }
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name = var.project
  }
}
