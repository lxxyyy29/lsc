# 无人机实时视频流代理方案

## 问题描述

无人机视频流无法在浏览器中直接播放，原因：

1. **URL 格式错误** - 无人机平台返回的 HLS 流地址格式有误：
   - 错误格式：`hls.m3u8?originTypeStr=rtmp_push?sign=xxx`
   - 正确格式：`hls.m3u8?originTypeStr=rtmp_push&sign=xxx`

2. **签名过期** - URL 中的 `sign` 参数有效期约 10 秒，过期后流中断

3. **跨域限制** - 浏览器无法直接连接外部 WebSocket FLV 流（`ws://8.156.93.151:11080`）

4. **认证头缺失** - hls.js 无法发送 `Authorization` 请求头

## 解决方案

### 架构

```
浏览器 → 后端代理(/api/drone/stream/proxy/{deviceSn}/hls.m3u8) → 无人机平台
```

### 后端实现

**文件：** `backend/src/main/java/com/changping/platform/modules/drone/controller/DroneStreamController.java`

核心功能：
- `GET /drone/stream/proxy/{deviceSn}/hls.m3u8` - 代理 HLS 播放列表
- `GET /drone/stream/proxy/{deviceSn}/seg` - 代理 TS 视频分片
- URL 格式自动修复（`?sign=` → `&sign=`）
- 每次请求都获取最新签名 URL

**文件：** `backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`

将流代理端点设为公开：
```java
.requestMatchers("/drone/stream/**")
.permitAll()
```

### 前端实现

**文件：** `web/src/views/DronesView.vue`

使用 hls.js 播放后端代理的 HLS 流：
```javascript
wsUrl.value = `/api/drone/stream/proxy/${deviceSn}/hls.m3u8`
const Hls = (await import('hls.js')).default
hlsPlayer = new Hls({ lowLatencyMode: true, ... })
hlsPlayer.loadSource(wsUrl.value)
hlsPlayer.attachMedia(video)
```

## 关键修复点

1. **URL 修复** - `fixUrlSign()` 方法将第二个 `?` 替换为 `&`
2. **公开端点** - 流代理不需要认证（hls.js 无法发送 header）
3. **后端代理** - 所有视频数据通过后端转发，避免浏览器直连外部服务

## 验证

```bash
# 测试播放列表代理
curl http://localhost:8080/api/drone/stream/proxy/8UUDM6400AY6S4/hls.m3u8

# 测试分片代理
curl http://localhost:8080/api/drone/stream/proxy/8UUDM6400AY6S4/seg?u=https://drone.kfktec.cn:9085/live/xxx.ts
```

## 注意事项

- HLS 流只在无人机实际推送视频时可用（飞行时）
- 签名有效期约 10 秒，后端每次请求都会刷新
- 支持 FLV over WebSocket 作为备选方案（需要后端代理 WebSocket）

## 已知问题/待修复

### 实时遥测数据 WebSocket 连接失败
- **现象**：视频正常播放，但实时飞行数据（电池、高度、速度等）不显示
- **原因**：后端连接无人机平台 WebSocket 时 token 过期（HTTP 400）
- **状态**：已修复 token 刷新逻辑（DroneApiClient.isTokenExpired），待验证
- **文件**：`backend/.../client/DroneApiClient.java`

### 视频偶尔跳跃
- **现象**：视频播放过程中偶尔跳跃或定格
- **状态**：已优化 HLS 播放器配置，待验证
