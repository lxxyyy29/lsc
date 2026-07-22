# 无人机AI智慧巡查平台 — 三方对接任务文档

> 来源：`东莞常平城管项目功能描述.numbers` + `无人机AI智慧巡查平台接口文档(v2.5).docx`
>
> 生成日期：2026-03-19
>
> 用途：后续新开会话逐步执行以下任务，完成后端代理层 + 前端页面对接

---

## 一、对接概述

项目功能描述中涉及三方对接的模块有 4 个（功能描述表序号 6/7/9/12）：

| 序号 | 模块 | 子模块 | 对接说明 |
|------|------|--------|----------|
| 6 | 无人机接入模块 | 设备管理 | 无人机设备档案，调用三方 API |
| 7 | 无人机接入模块 | 飞行/巡检任务管理 | 任务创建/计划，调用三方 API |
| 9 | 数据接入模块 | 媒体上传 | 图片/视频上传，调用三方 API |
| 12 | 模型管理 | 识别模型管理 | 模型版本/阈值，调用三方 API |

三方平台为 **大疆无人机AI智慧巡查平台**，通过 HTTP/HTTPS + WebSocket 接口通信。

---

## 二、三方平台认证机制

- 协议：HTTP/HTTPS
- 认证：除获取 Token 外，所有接口请求头携带 `x-auth-token`
- Token 获取：`POST /dj-prod-api/manage/api/v1/getToken`
- 密码加密：SM4 算法，secretKey=`gsis20232023gsis`，iv=`9na3v13cy9bt39vu`
- Token 应缓存并定期刷新

---

## 三、三方接口清单（共 20 个接口）

### A. 认证（1 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 1 | POST | `/dj-prod-api/manage/api/v1/getToken` | 获取 token，返回 `x-auth-token` 和 `region_code` |

### B. 工作空间/项目（1 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 5 | POST | `/dj-prod-api/manage/api/v1/workspaces/getWorkspaceListPageVo` | 获取工作空间列表（分页），入参 `region_code/page_num/page_size` |

### C. 设备管理（1 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 6 | POST | `/dj-prod-api/manage/api/v1/devices/getDockListPageVo` | 获取设备列表（分页），入参 `workspace_id/page_num/page_size`；返回机场+飞行器信息、视频流地址 |

### D. 航线管理（2 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 2 | GET | `/dj-prod-api/wayline/api/v1/workspaces/{workspaceId}/waylines?page=&page_size=` | 获取航线列表（分页） |
| 3 | GET | `/dj-prod-api/wayline/api/v1/workspaces/{workspaceId}/getWaylinePoint/{id}` | 获取航线航点经纬度列表 |

### E. 飞行任务管理（4 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 7 | POST | `/dj-prod-api/wayline/api/v1/workspaces/createImmediateJob` | 创建立即执行任务，入参 `workspace_id/dock_sn/file_id` |
| 8 | POST | `/dj-prod-api/wayline/api/v1/workspaces/pauseResumeJob` | 挂起/恢复任务，入参 `workspace_id/job_id/status(0挂起/1恢复)` |
| 9 | POST | `/dj-prod-api/wayline/api/v1/workspaces/returnHomeJob` | 立即返航，入参 `dock_sn` |
| 10 | POST | `/dj-prod-api/wayline/api/v1/workspaces/getJobListPageVo` | 获取任务列表（分页），入参 `workspace_id/page_num/page_size/status(可选)` |

### F. AI 算法模型（1 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 4 | POST | `/dj-prod-api/manage/api/v1/ai/getAlgorithmModelListPageVo` | 获取 AI 模型列表（分页），入参 `workspace_id/page_num/page_size` |

### G. 实时数据（WebSocket，1 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 11 | WS | `/dj-prod-api/api/v1/ws?x-auth-token=xxx&device_sn=xxx` | 接收实时数据，通过 `biz_code` 区分消息类型 |

**WebSocket biz_code 消息类型：**

| biz_code | 说明 | 关键字段 |
|----------|------|----------|
| `dock_osd` | 机场实时数据 | temperature, rainfall, wind_speed, height, longitude, latitude, modeCode(0空闲/1调试/2远程调试/3升级/4工作/99离线) |
| `device_osd` | 飞行器实时数据 | elevation, horizontal_speed, vertical_speed, height, longitude, latitude, modeCode(0待机~17指令飞行/99离线), battery.capacity_percent |
| `device_offline` | 飞机离线通知 | sn, online_status=false |
| `Flyline_Alg_Task_Staus` | AI 算法任务状态 | algTaskStaus[].status(0未计算/1计算中), currentWaypointIndex, isEnd, computingVideoPlayUrl（AI 实时画面地址） |

### H. 喊话器（5 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 12 | POST | `/dj-prod-api/manage/api/v1/speaker/psdk/pcmFile/getPcmFileListPageVo` | 获取喊话音频列表 |
| 13 | POST | `/dj-prod-api/manage/api/v1/speaker/psdk/{device_sn}/audioPlayStart` | 播放喊话音频 |
| 14 | POST | `/dj-prod-api/manage/api/v1/speaker/psdk/{device_sn}/stopPlay` | 停止喊话 |
| 15 | POST | `/dj-prod-api/manage/api/v1/speaker/psdk/pcmFile/upload` | 上传喊话音频（form-data） |
| 16 | GET | `/dj-prod-api/manage/api/v1/speaker/deleteById/{id}` | 删除喊话音频 |
| 17 | POST | `/dj-prod-api/manage/api/v1/speaker/psdk/{device_sn}/volumeSet` | 设置音量(0-100) |

### I. 负载控制/相机（3 个）

| # | 方法 | 路径 | 说明 |
|---|------|------|------|
| 18 | POST | `/dj-prod-api/control/api/v1/devices/{device_sn}/payload/commands` | 切换相机模式（cmd=`camera_mode_switch`，camera_mode: 0拍照/1录像/2智能低光/3全景） |
| 19 | POST | 同上 | 开始录像（cmd=`camera_recording_start`） |
| 20 | POST | 同上 | 停止录像（cmd=`camera_recording_stop`） |

---

## 四、对接任务拆分

### 任务 1：后端 — 三方 Token 管理与 HTTP 客户端

**目标**：创建统一的三方平台 HTTP 客户端，封装认证和请求逻辑。

**实现要点**：
1. 新建 `backend/src/main/java/com/changping/platform/modules/drone/` 模块
2. 配置类 `DroneApiConfig`：从 `application.yml` 读取三方平台 `serverAddr`、`wsAddr`、`username`、`password`
3. SM4 加密工具：实现密码加密（secretKey=`gsis20232023gsis`，iv=`9na3v13cy9bt39vu`）
4. `DroneApiClient`：封装 RestTemplate/WebClient
   - 自动获取 Token 并缓存（建议用 `@Cacheable` 或内存缓存，设置 TTL）
   - 所有请求自动附加 `x-auth-token` 请求头
   - 统一处理三方响应（code=0/200 为成功）
   - 存储 `region_code`（从 Token 接口返回）
5. `application.yml` 新增配置段（不硬编码凭据，使用环境变量）

**验收**：单元测试能成功获取 Token、调用一个列表接口返回数据。

---

### 任务 2：后端 — 工作空间与设备管理代理接口

**目标**：代理三方设备和工作空间接口，供前端调用。

**实现要点**：
1. `DroneWorkspaceController` — `GET /api/drone/workspaces`
   - 代理三方接口 5（获取工作空间列表）
   - 入参：`page`, `pageSize`（自动填入 `region_code`）
   - 返回：`{ id, workspaceId, workspaceName, workspaceDesc, regionCode, platformName, bindCode }`

2. `DroneDeviceController` — `GET /api/drone/devices`
   - 代理三方接口 6（获取设备列表）
   - 入参：`workspaceId`, `page`, `pageSize`
   - 返回关键字段：`{ id, deviceSn, deviceName, firmwareVersion, modeCode, longitude, latitude, videoPlayUrl, droneInfo: { droneSn, deviceName, modeCode, longitude, latitude, videoPlayUrl } }`

**前端页面字段对应**（来自功能描述）：
- 设备列表列：项目名称(`workspaceName`)、设备名称(`deviceName`)、设备SN(`deviceSn`)、固件版本(`firmwareVersion`)、状态(`modeCode` → 中文映射)

**验收**：前端能调用 `/api/drone/devices` 展示设备列表。

---

### 任务 3：后端 — 航线管理代理接口

**目标**：代理航线查询接口。

**实现要点**：
1. `DroneWaylineController`
   - `GET /api/drone/waylines` — 代理接口 2（航线列表），入参 `workspaceId/page/pageSize`
   - `GET /api/drone/waylines/:id/points` — 代理接口 3（航点经纬度），入参 `workspaceId/id`
   - 返回航线列表：`{ id, name, droneModelKey, updateTime }`
   - 返回航点列表：`{ waylines: [[lng, lat], ...] }`

**验收**：前端能加载航线列表并在地图上绘制航点路径。

---

### 任务 4：后端 — 飞行任务管理代理接口

**目标**：代理任务创建、控制、查询接口。

**实现要点**：
1. `DroneJobController`
   - `GET /api/drone/jobs` — 代理接口 10（任务列表），入参 `workspaceId/page/pageSize/status(可选)`
   - `POST /api/drone/jobs` — 代理接口 7（创建立即执行任务），入参 `workspaceId/dockSn/fileId`
   - `PUT /api/drone/jobs/:jobId/pause-resume` — 代理接口 8（挂起/恢复），入参 `workspaceId/status`
   - `POST /api/drone/jobs/return-home` — 代理接口 9（立即返航），入参 `dockSn`

**前端页面字段对应**（来自功能描述）：
- 任务列表列：计划起飞时间(`executeTime`)、实际时间(`beginTime`)、执行状态(`status` → 1待执行/2进行中/3成功/4取消/5失败)、任务名称(`jobName`)、类型(`taskType`)、航线名称(`fileName`)、设备名称(`dockName`)、创建人(`usernameCn`)

**验收**：前端能查看任务列表、创建飞行任务、挂起/恢复/返航。

---

### 任务 5：后端 — AI 算法模型管理代理接口

**目标**：代理 AI 模型查询接口。

**实现要点**：
1. `DroneAiModelController`
   - `GET /api/drone/ai-models` — 代理接口 4（AI 模型列表），入参 `workspaceId/page/pageSize`
   - 返回：`{ id, name, modelNo, labelList(JSON字符串→数组), status(0启用/1未启用), latestTrainingTime, onlineTime, createTime }`

**前端页面字段对应**（来自功能描述）：
- 模型列表列：模型名称(`name`)、序列号(`modelNo`)、最近训练时间(`latestTrainingTime`)、上线时间(`onlineTime`)、标签列表(`labelList`)、状态(`status`)

**验收**：前端 AI 模型配置页面展示真实模型数据。

---

### 任务 6：后端 — WebSocket 实时数据代理

**目标**：代理三方 WebSocket，将实时数据推送给前端。

**实现要点**：
1. 后端 WebSocket 客户端连接三方 `wss://serverAddr/dj-prod-api/api/v1/ws?x-auth-token=xxx&device_sn=xxx`
2. 后端 WebSocket 服务端 `/api/ws/drone?deviceSn=xxx` 供前端连接
3. 按 `biz_code` 分类转发：
   - `dock_osd`：机场状态（温度、降雨量、风速、位置、modeCode）
   - `device_osd`：飞行器状态（高度、速度、位置、电量、modeCode）
   - `device_offline`：飞机离线通知
   - `Flyline_Alg_Task_Staus`：AI 任务状态（含 AI 实时画面地址 `computingVideoPlayUrl`）
4. 考虑断线重连、Token 过期自动刷新

**验收**：前端连接 WebSocket 能实时显示飞行器位置和 AI 任务状态。

---

### 任务 7：后端 — 喊话器与负载控制代理接口

**目标**：代理喊话器和相机控制接口。

**实现要点**：
1. `DroneSpeakerController`
   - `GET /api/drone/speaker/files` — 音频列表（接口 12）
   - `POST /api/drone/speaker/files` — 上传音频（接口 15，form-data 转发）
   - `DELETE /api/drone/speaker/files/:id` — 删除音频（接口 16）
   - `POST /api/drone/speaker/:deviceSn/play` — 播放（接口 13）
   - `POST /api/drone/speaker/:deviceSn/stop` — 停止（接口 14）
   - `POST /api/drone/speaker/:deviceSn/volume` — 设置音量（接口 17）

2. `DronePayloadController`
   - `POST /api/drone/devices/:deviceSn/camera/mode` — 切换相机模式（接口 18）
   - `POST /api/drone/devices/:deviceSn/camera/record-start` — 开始录像（接口 19）
   - `POST /api/drone/devices/:deviceSn/camera/record-stop` — 停止录像（接口 20）

**验收**：能通过前端控制喊话器播放/停止/音量，控制相机录像。

---

### 任务 8：前端 — 设备管理页面对接

**目标**：将无人机设备管理页面对接真实 API。

**实现要点**：
1. 新建 `web/src/api/drone.ts`：封装所有无人机相关 API 调用
2. 改造现有无人机/设备页面（或新建）：
   - 设备列表：项目名称、设备名称、设备SN、固件版本、状态
   - 设备状态用 modeCode 映射中文：0空闲/1调试/2远程调试/3升级/4工作/99离线
   - 视频流播放入口（后续任务处理播放器）
3. 删除硬编码数据，`onMounted` 加载真实设备列表

**验收**：登录后能看到真实设备列表。

---

### 任务 9：前端 — 飞行任务管理页面对接

**目标**：将飞行/巡检任务页面对接真实 API。

**实现要点**：
1. 任务列表页：加载真实任务数据
   - 列：计划起飞时间、实际时间、执行状态、任务名称、类型、航线名称、设备名称、创建人
   - 状态过滤（全部/待执行/进行中/成功/取消/失败）
2. 创建任务对话框：
   - 选择设备（dock_sn）→ 选择航线（file_id）→ 确认创建
3. 任务控制（列表行操作）：挂起/恢复、立即返航

**验收**：能创建飞行任务、查看任务列表、执行挂起/恢复/返航操作。

---

### 任务 10：前端 — AI 模型管理页面对接

**目标**：将 AI 模型配置页面对接真实 API。

**实现要点**：
1. 改造 `AIModelConfigView.vue`（已有页面）
2. 加载真实模型列表：模型名称、序列号、标签列表（展示为 Tag 组）、最近训练时间、上线时间、状态
3. `labelList` 是 JSON 字符串，前端需 `JSON.parse` 后展示为标签

**验收**：AI 模型配置页展示三方平台真实模型数据。

---

### 任务 11：前端 — WebSocket 实时数据展示

**目标**：前端连接 WebSocket，实时展示飞行状态。

**实现要点**：
1. 新建 `web/src/composables/useDroneWebSocket.ts` 封装 WebSocket 连接
2. 在设备详情/飞行监控页面中使用：
   - 机场状态面板：温度、降雨量、风速、工作状态
   - 飞行器状态面板：高度、速度、位置、电量、飞行模式
   - AI 任务面板：当前航点、算法状态、实时 AI 画面播放入口
3. 地图上实时标注飞行器位置（经纬度 → 地图标点）
4. 处理离线通知（`device_offline`）更新设备状态

**验收**：飞行时前端能实时看到飞行器位置、高度、速度和 AI 画面状态。

---

### 任务 12：前端 — 媒体数据展示（预留）

**目标**：预留媒体数据（图片/视频）展示入口。

**实现要点**：
1. 三方接口文档中暂未提供独立的媒体列表接口，但任务接口返回了 `media_count/uploaded_count`
2. 前端预留媒体列表页面结构
3. 表格列：文件名、图片/视频类型、无人机、拍摄负载、创建时间
4. 后续三方提供媒体接口后再完成数据对接

**验收**：页面框架存在，列表为空状态提示。

---

## 五、执行顺序建议

```
任务 1（Token + HTTP 客户端）
  ├── 任务 2（设备） → 任务 8（前端设备页）
  ├── 任务 3（航线）
  ├── 任务 4（任务） → 任务 9（前端任务页）
  ├── 任务 5（AI模型） → 任务 10（前端AI模型页）
  ├── 任务 6（WebSocket） → 任务 11（前端实时数据）
  ├── 任务 7（喊话器+相机）
  └── 任务 12（媒体预留）
```

**关键路径**：任务 1 → 任务 2/3/4/5（可并行） → 前端页面对接（可并行）

---

## 六、配置项模板

在 `application.yml` 中新增（示例，实际凭据用环境变量）：

```yaml
drone:
  api:
    server-addr: ${DRONE_API_SERVER:https://uav.kfktec.cn}
    ws-addr: ${DRONE_WS_SERVER:wss://uav.kfktec.cn}
    username: ${DRONE_API_USERNAME:}
    password: ${DRONE_API_PASSWORD:}
    sm4-secret-key: gsis20232023gsis
    sm4-iv: 9na3v13cy9bt39vu
```

---

## 七、注意事项

1. **三方响应格式**：`{ code: 0/200, message: "success", data: {...} }`，分页在 `data.pagination` 中
2. **时间戳**：三方返回的时间字段（`createTime`、`updateTime`、`latestTrainingTime` 等）为毫秒级 Unix 时间戳，需前端格式化
3. **SM4 加密**：获取 Token 时密码需先加密再传输，Java 可用 Bouncy Castle 库
4. **WebSocket 消息量大**：飞行器实时数据推送频率高（约每秒），前端注意节流渲染
5. **视频流**：返回的视频地址有 HLS(`.m3u8`)、FLV(`wss://.../xxx.live.flv`)、RTMP 三种格式，前端播放建议用 FLV.js 或 HLS.js
6. **状态码映射**：机场和飞行器的 `modeCode` 含义不同，需分别映射
