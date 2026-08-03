# ============================================
# Docker 部署管理命令
# ============================================

.PHONY: help build up down restart logs clean status

# 默认目标
help:
	@echo "========================================"
	@echo "  网格社区治理平台 - Docker 部署命令"
	@echo "========================================"
	@echo ""
	@echo "  make build    - 构建所有镜像"
	@echo "  make up       - 启动所有服务"
	@echo "  make down     - 停止所有服务（仅本项目）"
	@echo "  make restart  - 重启所有服务"
	@echo "  make logs     - 查看日志"
	@echo "  make status   - 查看服务状态"
	@echo "  make clean    - 清理本项目容器（⚠️ 不影响其他项目）"
	@echo ""

# 构建镜像
build:
	@echo "🔨 构建镜像..."
	docker compose build --no-cache

# 启动服务
up:
	@echo "🚀 启动服务..."
	docker compose up -d
	@echo ""
	@echo "✅ 服务已启动！"
	@echo "  Web 管理端: http://localhost:10080"
	@echo "  H5 移动端:  http://localhost:10082"
	@echo "  小程序:     http://localhost:10083"
	@echo ""

# 停止服务（仅本项目容器）
down:
	@echo "🛑 停止本项目服务..."
	docker compose down

# 重启服务
restart:
	@echo "🔄 重启服务..."
	docker compose restart

# 查看日志
logs:
	docker compose logs -f --tail=100

# 查看后端日志
logs-backend:
	docker compose logs -f changping-backend

# 查看服务状态
status:
	docker compose ps

# 清理本项目（⚠️ 不影响其他项目）
clean:
	@echo "⚠️  警告：将删除本项目所有容器！"
	@read -p "确认继续? [y/N] " confirm && [ $$confirm = y ] && docker compose down --rmi all

# 进入后端容器
shell-backend:
	docker exec -it changping-backend sh

# 数据库控制台
mysql-cli:
	docker exec -it changping-mysql mysql -u root -p$${MYSQL_ROOT_PASSWORD} zhsq

# Redis 控制台
redis-cli:
	docker exec -it changping-redis redis-cli -a $${REDIS_PASSWORD}
