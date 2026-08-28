#!/bin/bash
# ============================================
# 东莞杰瑞智慧网格治理平台 - 一键部署脚本
# 在阿里云服务器上执行此脚本
# ============================================

set -e

echo "============================================"
echo "  东莞杰瑞智慧网格治理平台 - Docker 部署"
echo "============================================"

# 配置
PROJECT_DIR="/opt/changping"
DOCKER_COMPOSE_VERSION="2.20.0"

# 1. 安装 Docker（如已存在则跳过）
install_docker() {
    if command -v docker &> /dev/null; then
        echo "[✓] Docker 已安装: $(docker --version)"
        return
    fi
    echo "[...] 安装 Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker
    systemctl start docker
    echo "[✓] Docker 安装完成"
}

# 2. 安装 Docker Compose（如已存在则跳过）
install_compose() {
    if docker compose version &> /dev/null; then
        echo "[✓] Docker Compose 已安装: $(docker compose version)"
        return
    fi
    echo "[...] 安装 Docker Compose..."
    curl -L "https://github.com/docker/compose/releases/download/v${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    echo "[✓] Docker Compose 安装完成"
}

# 3. 创建项目目录
setup_project() {
    echo "[...] 创建项目目录..."
    mkdir -p ${PROJECT_DIR}
    mkdir -p /home/docker/uav/changping/data/{mysql,redis,mongodb,minio}
    mkdir -p /home/docker/uav/changping/uploads
    echo "[✓] 目录创建完成"
}

# 4. 创建环境变量文件
create_env() {
    if [ -f "${PROJECT_DIR}/.env" ]; then
        echo "[✓] .env 文件已存在，跳过"
        return
    fi
    echo "[...] 创建 .env 文件..."
    cat > ${PROJECT_DIR}/.env << 'ENVEOF'
# 数据库密码（请修改为强密码）
MYSQL_ROOT_PASSWORD=Changping@2026

# Redis 密码
REDIS_PASSWORD=Changping@2026

# MinIO 配置
MINIO_USER=admin
MINIO_PASSWORD=Changping@2026

# JWT 密钥（必须修改为随机字符串）
JWT_SECRET=ChangpingGridCommunity2026SecretKeyPleaseChangeThis

# 域名（修改为你的域名或IP）
DOMAIN=http://8.156.93.151

# 端口配置
WEB_PORT=10080
H5_PORT=10082
MP_PORT=10083
BACKEND_PORT=10081
ENVEOF
    echo "[✓] .env 文件创建完成（请修改密码）"
}

# 5. 拉取代码
pull_code() {
    if [ -d "${PROJECT_DIR}/.git" ]; then
        echo "[...] 更新代码..."
        cd ${PROJECT_DIR}
        git pull origin master
    else
        echo "[...] 克隆代码..."
        git clone https://github.com/lxxyyy29/lsc.git ${PROJECT_DIR}
    fi
    echo "[✓] 代码就绪"
}

# 6. 构建并启动
deploy() {
    echo "[...] 构建并启动容器（首次需要 5-10 分钟）..."
    cd ${PROJECT_DIR}
    docker-compose build --no-cache
    docker-compose up -d
    echo "[✓] 容器启动完成"
}

# 7. 等待服务就绪
wait_ready() {
    echo "[...] 等待服务就绪..."
    for i in $(seq 1 30); do
        if curl -s http://localhost:10081/api/auth/login -X POST -H "Content-Type: application/json" -d '{"account":"health","password":"check"}' > /dev/null 2>&1; then
            echo "[✓] 后端服务就绪"
            return
        fi
        echo "    等待中... ($i/30)"
        sleep 5
    done
    echo "[!] 服务启动超时，请检查日志: docker-compose logs"
}

# 8. 显示状态
show_status() {
    echo ""
    echo "============================================"
    echo "  部署完成！"
    echo "============================================"
    echo ""
    echo "服务地址："
    echo "  Web 管理端:  http://8.156.93.151:10080"
    echo "  H5 移动端:   http://8.156.93.151:10082"
    echo "  小程序:      http://8.156.93.151:10083"
    echo "  后端 API:    http://8.156.93.151:10081/api"
    echo ""
    echo "常用命令："
    echo "  查看日志:  docker-compose logs -f"
    echo "  重启服务:  docker-compose restart"
    echo "  停止服务:  docker-compose down"
    echo "  查看状态:  docker-compose ps"
    echo ""
    echo "默认账号: admin / admin123"
    echo ""
}

# 执行
install_docker
install_compose
setup_project
create_env
pull_code
deploy
wait_ready
show_status
