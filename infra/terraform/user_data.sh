#!/bin/bash
# EC2 최초 부팅 1회 실행 (root). 로그: /var/log/cloud-init-output.log
set -euxo pipefail

export DEBIAN_FRONTEND=noninteractive

# ── Docker + Compose v2 ──────────────────────────────────────────
apt-get update -y
apt-get install -y docker.io docker-compose-v2

# 컨테이너 로그 무제한 증가로 디스크가 차는 것 방지
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF

systemctl enable docker
systemctl restart docker
usermod -aG docker ubuntu

# ── swap 2GB (t3.small 2GB 메모리 보완) ───────────────────────────
if [ ! -f /swapfile ]; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# ── Elasticsearch 커널 파라미터 ──────────────────────────────────
cat > /etc/sysctl.d/99-elasticsearch.conf <<'EOF'
vm.max_map_count=262144
vm.swappiness=10
EOF
sysctl --system

# ── 배포 디렉터리 (compose.yaml / .env / nginx / certbot) ─────────
install -d -o ubuntu -g ubuntu /home/ubuntu/blog
install -d -o ubuntu -g ubuntu /home/ubuntu/blog/nginx
install -d -o ubuntu -g ubuntu /home/ubuntu/blog/elasticsearch
install -d -o ubuntu -g ubuntu /home/ubuntu/blog/certbot/conf
install -d -o ubuntu -g ubuntu /home/ubuntu/blog/certbot/www
