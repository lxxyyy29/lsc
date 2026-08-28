# Docker 部署指南

## ⚠️ 端口注意事项

当前服务器已有多个项目运行，**必须使用以下端口**：

| 服务 | 端口 | 说明 |
|------|------|------|
| Web 管理端 | 10080 | Nginx 前端 |
| 后端 API | 10081 | Spring Boot |
| H5 移动端 | 10082 | uni-app H5 |
| 小程序 | 10083 | Vue3 小程序 |

**数据库/Redis/MinIO 不映射宿主机端口**，仅在 Docker 网络内部访问。

## 快速开始

### 1. 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 2. 部署步骤

```bash
# 进入 docker 目录
cd docker

# 复制环境变量配置
cp .env.example .env

# ⚠️ 编辑 .env 文件，修改所有密码和 JWT 密钥！
vim .env

# 构建并启动所有服务
docker compose up -d --build

# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 3. 访问地址

| 服务 | 地址 |
|------|------|
| Web 管理端 | http://服务器IP:10080 |
| H5 移动端 | http://服务器IP:10082 |
| 小程序 | http://服务器IP:10083 |
| 后端 API | http://服务器IP:10081/api |

### 4. 测试账号

| 角色 | 账号 | 密码 |
|------|------|------|
| Web 管理员 | admin | admin123 |
| H5 网格员 | grid01 | 123456 |

## 常用命令

```bash
# 查看日志
docker compose logs -f changping-backend

# 重启服务
docker compose restart

# 停止本项目服务（不影响其他项目）
docker compose down

# 备份数据库
docker exec changping-mysql mysqldump -u root -p密码 zhsq > backup.sql

# 进入容器
docker exec -it changping-backend sh
```

## 数据持久化

所有数据存储在服务器独立目录：

```
/home/docker/uav/changping/
├── data/
│   ├── mysql/       # MySQL 数据
│   ├── redis/       # Redis 数据
│   ├── mongodb/     # MongoDB 数据
│   └── minio/       # MinIO 文件存储
└── uploads/         # 上传文件
```

## ⚠️ 重要提醒

1. **不要执行全局 docker 命令**：`docker stop $(docker ps -q)` 会停止所有项目
2. **只在本项目目录执行** `docker compose` 命令
3. **修改密码**：生产环境必须修改 `.env` 中的所有密码
4. **防火墙**：部署后需要在阿里云安全组开放 10080-10083 端口
5. **日志轮转**：已配置单容器日志最大 100MB，保留 3 个文件

## 故障排查

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs --tail=100

# 检查端口占用
ss -tulpen | grep 10080

# 重启单个服务
docker compose restart changping-backend
```
