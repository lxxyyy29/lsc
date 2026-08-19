#!/bin/bash
# ============================================
# 在阿里云服务器上通过 XShell 执行此脚本
# 一键部署东莞杰瑞智慧网格治理平台
# ============================================

set -e

echo "============================================"
echo "  东莞杰瑞智慧网格治理平台 - Docker 部署"
echo "============================================"

# 0. 安装 Docker（如已存在则跳过）
if ! command -v docker &> /dev/null; then
    echo "[1/6] 安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
else
    echo "[1/6] Docker 已安装: $(docker --version)"
fi

# 1. 安装 Docker Compose
if ! docker compose version &> /dev/null 2>&1; then
    echo "[2/6] 安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
else
    echo "[2/6] Docker Compose 已安装"
fi

# 2. 创建项目目录
echo "[3/6] 创建项目目录..."
mkdir -p /opt/changping
mkdir -p /home/docker/uav/changping/data/{mysql,redis,mongodb,minio}
mkdir -p /home/docker/uav/changping/uploads

# 3. 克隆代码
cd /opt/changping
if [ -d ".git" ]; then
    echo "[4/6] 更新代码..."
    git pull origin master
else
    echo "[4/6] 克隆代码..."
    git clone https://github.com/lxxyyy29/lsc.git .
fi

# 4. 创建环境变量
if [ ! -f ".env" ]; then
    # 生成随机密码
    MYSQL_PASS=$(openssl rand -base64 24 | tr -dc 'a-zA-Z0-9!@#$%^&*' | head -c 24)
    REDIS_PASS=$(openssl rand -base64 24 | tr -dc 'a-zA-Z0-9!@#$%^&*' | head -c 24)
    MINIO_PASS=$(openssl rand -base64 24 | tr -dc 'a-zA-Z0-9!@#$%^&*' | head -c 24)
    JWT_SECRET=$(openssl rand -base64 48 | tr -dc 'a-zA-Z0-9' | head -c 48)

    cat > .env << EOF
# 自动生成于 $(date '+%Y-%m-%d %H:%M:%S')
# ⚠️ 请妥善保管此文件，不要提交到 Git

# 端口配置（已确认与现有服务无冲突）
WEB_PORT=10080
H5_PORT=10082
MP_PORT=10083
BACKEND_PORT=10081

# MySQL（已自动生成强密码）
MYSQL_ROOT_PASSWORD=${MYSQL_PASS}

# Redis
REDIS_PASSWORD=${REDIS_PASS}

# MinIO
MINIO_USER=admin
MINIO_PASSWORD=${MINIO_PASS}

# JWT 密钥
JWT_SECRET=${JWT_SECRET}

# 访问地址
DOMAIN=http://8.156.93.151:10080
EOF
    chmod 600 .env
    echo "[✓] .env 文件已创建（密码已自动生成，权限已设为 600）"
else
    echo "[✓] .env 文件已存在"
fi

# 5. 构建并启动
echo "[5/6] 构建并启动（首次 5-10 分钟）..."
docker compose build --no-cache
docker compose up -d

# 6. 等待就绪
echo "[6/6] 等待服务就绪..."
for i in $(seq 1 30); do
    if curl -s http://localhost:10081/api/auth/login -X POST -H "Content-Type: application/json" -d '{"account":"health","password":"check"}' > /dev/null 2>&1; then
        echo ""
        echo "============================================"
        echo "  部署完成！"
        echo "============================================"
        echo ""
        echo "访问地址："
        echo "  Web 管理端:  http://8.156.93.151:10080"
        echo "  H5 移动端:   http://8.156.93.151:10082"
        echo "  小程序:      http://8.156.93.151:10083"
        echo "  后端 API:    http://8.156.93.151:10081/api"
        echo ""
        echo "默认账号: admin / admin123"
        echo ""
        echo "⚠️  安全提醒："
        echo "  1. 数据库/Redis/MongoDB 端口未映射到宿主机"
        echo "  2. .env 文件包含敏感信息，请勿提交到 Git"
        echo "  3. 建议配置阿里云安全组，仅开放 10080-10083"
        echo ""
        echo "常用命令："
        echo "  查看日志:  docker compose logs -f"
        echo "  重启:      docker compose restart"
        echo "  停止:      docker compose down"
        echo "  查看状态:  docker compose ps"
        echo ""
        exit 0
    fi
    printf "    等待中... (%d/30)\n" $i
    sleep 5
done

echo "[!] 服务启动超时，请检查: docker compose logs"
