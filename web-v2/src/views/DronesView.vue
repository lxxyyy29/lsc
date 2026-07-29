<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">无人机与视频监控</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">九大巡检板块、AI自动识别预警、外部视频源接入</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">无人机总数</p>
        <p class="stat-value">{{ overview.totalDevices || 0 }}</p>
        <p style="font-size:11px;color:#52c41a;">在线: {{ overview.onlineDevices || 0 }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">任务总数</p>
        <p class="stat-value">{{ overview.totalJobs || 0 }}</p>
        <p style="font-size:11px;color:#faad14;">执行中: {{ overview.runningJobs || 0 }}</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">航线数</p>
        <p class="stat-value">{{ overview.totalWaylines || 0 }}</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">AI告警</p>
        <p class="stat-value">{{ overview.aiAlerts || 0 }}</p>
      </div>
      <div class="card card-border-blue">
        <p class="stat-label">今日巡查</p>
        <p class="stat-value">{{ overview.todayInspections || 0 }}</p>
      </div>
    </div>

    <!-- 标签页 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;border-bottom:1px solid #e5e7eb;padding-bottom:8px;">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
              :style="{padding:'6px 16px',border:'none',borderRadius:'6px',fontSize:'13px',cursor:'pointer',background: activeTab === tab.key ? '#1890ff' : '#f3f4f6',color: activeTab === tab.key ? '#fff' : '#374151'}">
        {{ tab.label }}
      </button>
    </div>

    <!-- 设备管理 -->
    <div v-if="activeTab === 'devices'" class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">无人机设备（外部数据源）</h3>
      <table class="table">
        <thead><tr><th>设备名称</th><th>类型</th><th>状态</th><th>序列号</th></tr></thead>
        <tbody>
          <tr v-for="d in devices" :key="d.id || d.deviceSn || d.device_sn">
            <td>{{ d.deviceName || d.nickname || d.name || '-' }}</td>
            <td><span class="tag tag-blue">{{ d.deviceType === 3 ? '无人机' : '机场' }}</span></td>
            <td><span :class="['tag', d.boundStatus === true ? 'tag-green' : 'tag-orange']">{{ d.boundStatus === true ? '在线' : '离线' }}</span></td>
            <td style="font-size:12px;">{{ d.deviceSn || d.childSn || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!devices.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无设备数据（请检查外部无人机平台连接）</p>
    </div>

    <!-- 巡检任务 -->
    <div v-if="activeTab === 'jobs'" class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">巡检任务</h3>
        <div style="display:flex;gap:8px;">
          <select v-model="jobStatusFilter" @change="loadData" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
            <option :value="undefined">全部状态</option>
            <option :value="0">待执行</option>
            <option :value="1">执行中</option>
            <option :value="2">已完成</option>
            <option :value="3">已完成</option>
            <option :value="4">已取消</option>
            <option :value="5">执行失败</option>
          </select>
          <button @click="showCreateJob = true" style="padding:4px 10px;border:1px solid #1890ff;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">
            <i class="fas fa-plus"></i> 创建任务
          </button>
        </div>
      </div>
      <table class="table">
        <thead><tr><th>任务名称</th><th>航线名称</th><th>设备</th><th>执行时间</th><th>完成时间</th><th>媒体</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="j in jobs" :key="j.job_id || j.id">
            <td>{{ j.job_name || j.jobName || '-' }}</td>
            <td style="font-size:12px;">{{ j.file_name || '-' }}</td>
            <td style="font-size:12px;">{{ j.dock_name || '-' }}</td>
            <td style="font-size:12px;">{{ j.execute_time || '-' }}</td>
            <td style="font-size:12px;">{{ j.completed_time || '-' }}</td>
            <td style="font-size:12px;">{{ j.uploaded_count ?? 0 }}/{{ j.media_count ?? 0 }}</td>
            <td><span :class="['tag', jobStatusClass(j)]">{{ jobStatusLabel(j) }}</span></td>
            <td>
              <div style="display:flex;gap:4px;">
                <button v-if="j.status === 1" @click="pauseJob(j.job_id || j.id)" style="padding:2px 6px;border:1px solid #faad14;border-radius:4px;background:#fff;color:#faad14;font-size:10px;cursor:pointer;">挂起</button>
                <button v-if="j.status === 0" @click="resumeJob(j.job_id || j.id)" style="padding:2px 6px;border:1px solid #52c41a;border-radius:4px;background:#fff;color:#52c41a;font-size:10px;cursor:pointer;">恢复</button>
                <button v-if="j.status === 0 || j.status === 1" @click="returnHomeJob(j.dock_sn)" style="padding:2px 6px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;font-size:10px;cursor:pointer;">返航</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!jobs.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无任务</p>
    </div>

    <!-- 创建任务对话框 -->
    <div v-if="showCreateJob" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:420px;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">创建飞行任务</h3>
        <div style="margin-bottom:12px;">
          <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">选择设备（机巢）</label>
          <select v-model="newJob.dockSn" style="width:100%;padding:8px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="">请选择设备</option>
            <option v-for="d in devices" :key="d.deviceSn" :value="d.deviceSn">
              {{ d.deviceName || d.nickname }} ({{ d.deviceSn }})
            </option>
          </select>
        </div>
        <div style="margin-bottom:16px;">
          <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">选择航线</label>
          <select v-model="newJob.fileId" style="width:100%;padding:8px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="">请选择航线</option>
            <option v-for="w in waylines" :key="w.id || w.waylineId || w.file_id" :value="w.id || w.waylineId || w.file_id">
              {{ w.waylineName || w.name || w.file_name || '-' }}
            </option>
          </select>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;">
          <button @click="showCreateJob = false" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="createNewJob" :disabled="!newJob.dockSn || !newJob.fileId" style="padding:6px 16px;border:1px solid #1890ff;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
            确认创建
          </button>
        </div>
      </div>
    </div>

    <!-- 航线管理 -->
    <div v-if="activeTab === 'waylines'" class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">巡检航线</h3>
      <table class="table">
        <thead><tr><th>航线名称</th><th>类型</th><th>更新时间</th></tr></thead>
        <tbody>
          <tr v-for="w in waylines" :key="w.id || w.waylineId">
            <td>{{ w.waylineName || w.name || '-' }}</td>
            <td><span class="tag tag-blue">{{ w.waylineType || w.type || '点状航线' }}</span></td>
            <td style="font-size:12px;">{{ w.updateTime || w.updated_at || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!waylines.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无航线</p>
    </div>

    <!-- 实时态势（地图+视频） -->
    <div v-if="activeTab === 'video'">
      <!-- 地图 + 视频 并排 -->
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px;">
        <!-- 地图 -->
        <div class="card" style="padding:0;overflow:hidden;">
          <div style="padding:12px 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e5e7eb;">
            <h3 style="font-size:14px;font-weight:600;"><i class="fas fa-map-marked-alt" style="color:#1890ff;margin-right:6px;"></i>实时位置</h3>
            <span style="font-size:11px;color:#6b7280;">点击标记查看视频</span>
          </div>
          <div id="droneMap" style="height:320px;"></div>
        </div>

        <!-- 视频播放器 -->
        <div class="card" style="padding:0;overflow:hidden;">
          <div style="padding:12px 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e5e7eb;">
            <h3 style="font-size:14px;font-weight:600;"><i class="fas fa-video" style="color:#1890ff;margin-right:6px;"></i>{{ selectedDrone ? (selectedDrone.deviceName || selectedDrone.nickname) : '实时视频' }}</h3>
            <span v-if="selectedDrone && wsUrl" style="display:flex;align-items:center;gap:4px;font-size:11px;color:#52c41a;">
              <span style="width:6px;height:6px;background:#52c41a;border-radius:50%;display:inline-block;animation:pulse 1.5s infinite;"></span>
              直播中
            </span>
          </div>
          <div style="padding:12px;">
            <div v-if="selectedDrone && wsUrl">
              <video id="droneVideo" controls autoplay muted playsinline
                     style="width:100%;height:260px;border-radius:8px;background:#000;object-fit:cover;"></video>
              <div style="display:flex;justify-content:space-between;margin-top:8px;font-size:11px;">
                <span style="color:#6b7280;"><i class="fas fa-map-marker-alt"></i> {{ selectedDrone.longitude }}, {{ selectedDrone.latitude }}</span>
                <span class="tag tag-blue">{{ streamType }} · 持续推流</span>
              </div>
            </div>
            <div v-else style="height:260px;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#9ca3af;background:#f9fafb;border-radius:8px;">
              <i class="fas fa-satellite-dish" style="font-size:36px;margin-bottom:12px;color:#d1d5db;"></i>
              <p style="font-size:13px;">选择左侧设备查看实时视频</p>
              <p style="font-size:11px;margin-top:4px;">WebSocket FLV 低延迟流 (1-3秒)</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 实时飞行数据 -->
      <div v-if="selectedDrone" class="card" style="margin-bottom:16px;">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
          <h3 style="font-size:14px;font-weight:600;"><i class="fas fa-satellite-dish" style="color:#1890ff;margin-right:6px;"></i>实时飞行数据</h3>
          <span :style="{display:'flex',alignItems:'center',gap:'4px',fontSize:11,color:wsConnected ? '#52c41a' : '#ff4d4f'}">
            <span :style="{width:6,height:6,background:wsConnected ? '#52c41a' : '#ff4d4f',borderRadius:'50%',display:'inline-block'}"></span>
            {{ wsConnected ? 'WebSocket 已连接' : '连接中...' }}
          </span>
        </div>
        <div style="display:grid;grid-template-columns:repeat(6,1fr);gap:12px;">
          <div style="text-align:center;padding:8px;background:#f0f9ff;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">🔋 电池电量</p>
            <p style="font-size:18px;font-weight:700;color:#1890ff;">{{ realtimeData.battery || '-' }}<span style="font-size:11px;">%</span></p>
          </div>
          <div style="text-align:center;padding:8px;background:#f6ffed;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">📏 飞行高度</p>
            <p style="font-size:18px;font-weight:700;color:#52c41a;">{{ realtimeData.height || '-' }}<span style="font-size:11px;">m</span></p>
          </div>
          <div style="text-align:center;padding:8px;background:#fff7e6;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">🚀 飞行速度</p>
            <p style="font-size:18px;font-weight:700;color:#fa8c16;">{{ realtimeData.speed || '-' }}<span style="font-size:11px;">m/s</span></p>
          </div>
          <div style="text-align:center;padding:8px;background:#fff0f6;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">📍 经度</p>
            <p style="font-size:14px;font-weight:600;color:#eb2f96;">{{ realtimeData.longitude ? Number(realtimeData.longitude).toFixed(5) : '-' }}</p>
          </div>
          <div style="text-align:center;padding:8px;background:#f9f0ff;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">📍 纬度</p>
            <p style="font-size:14px;font-weight:600;color:#722ed1;">{{ realtimeData.latitude ? Number(realtimeData.latitude).toFixed(5) : '-' }}</p>
          </div>
          <div style="text-align:center;padding:8px;background:#e6f7ff;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;margin-bottom:4px;">⚙️ 飞行模式</p>
            <p style="font-size:13px;font-weight:600;color:#096dd9;">{{ realtimeData.mode || '-' }}</p>
          </div>
        </div>
        <!-- 机场环境数据 -->
        <div v-if="realtimeData.temperature !== undefined" style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-top:8px;">
          <div style="text-align:center;padding:6px;background:#fafafa;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;">🌡️ 温度: {{ realtimeData.temperature }}°C</p>
          </div>
          <div style="text-align:center;padding:6px;background:#fafafa;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;">💨 风速: {{ realtimeData.windSpeed }} m/s</p>
          </div>
          <div style="text-align:center;padding:6px;background:#fafafa;border-radius:6px;">
            <p style="font-size:10px;color:#6b7280;">🌧️ 降雨: {{ realtimeData.rainfall }} mm</p>
          </div>
        </div>
      </div>

      <!-- 设备列表 -->
      <div class="card">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
          <h3 style="font-size:14px;font-weight:600;">设备列表 ({{ devices.length }})</h3>
          <button @click="loadData" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
            <i class="fas fa-sync"></i> 刷新
          </button>
        </div>
        <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:10px;">
          <div v-for="d in devices" :key="d.id || d.deviceSn || d.device_sn" @click="selectDrone(d)"
               :style="{padding:'10px',border:'2px solid ' + (selectedDrone && (selectedDrone.deviceSn === d.deviceSn || selectedDrone.id === d.id) ? '#1890ff' : '#e5e7eb'),borderRadius:'8px',cursor:'pointer',background: selectedDrone && (selectedDrone.deviceSn === d.deviceSn || selectedDrone.id === d.id) ? '#e6f4ff' : '#fff',transition:'all 0.2s'}">
            <div style="display:flex;align-items:center;gap:6px;margin-bottom:4px;">
              <i class="fas" :class="d.deviceType === 3 ? 'fa-helicopter' : 'fa-charging-station'" :style="{color: d.boundStatus ? '#52c41a' : '#ff4d4f',fontSize:'14px'}"></i>
              <span style="font-size:12px;font-weight:600;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ d.deviceName || d.nickname || '-' }}</span>
            </div>
            <p style="font-size:10px;color:#9ca3af;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ d.deviceSn || d.childSn || '-' }}</p>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:4px;">
              <span style="font-size:9px;color:#6b7280;">{{ d.longitude ? d.longitude.toFixed(3) : '-' }}, {{ d.latitude ? d.latitude.toFixed(3) : '-' }}</span>
              <span :class="['tag', d.boundStatus ? 'tag-green' : 'tag-red']" style="font-size:9px;padding:1px 6px;">{{ d.boundStatus ? '在线' : '离线' }}</span>
            </div>
          </div>
        </div>
        <p v-if="!devices.length" style="text-align:center;padding:30px;color:#9ca3af;font-size:12px;">暂无设备数据</p>
      </div>
    </div>

    <!-- AI告警 -->
    <div v-if="activeTab === 'alerts'" class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">AI识别告警</h3>
      <table class="table">
        <thead><tr><th>事件编号</th><th>标题</th><th>类型</th><th>紧急程度</th><th>状态</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="a in aiAlerts" :key="a.event_code">
            <td style="font-size:12px;">{{ a.event_code }}</td>
            <td>{{ a.title }}</td>
            <td><span class="tag tag-blue">{{ getEventTypeName(a.event_type) }}</span></td>
            <td><span :class="['tag', a.urgency_level === 'RED' ? 'tag-red' : a.urgency_level === 'YELLOW' ? 'tag-orange' : 'tag-green']">{{ a.urgency_level }}</span></td>
            <td><span :class="['tag', a.status === 'CLOSED' ? 'tag-green' : 'tag-orange']">{{ a.status }}</span></td>
            <td style="font-size:12px;">{{ a.occurred_at }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!aiAlerts.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无AI告警</p>
    </div>

    <!-- 巡检内容 -->
    <div v-if="activeTab === 'patrol'" class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">九大巡检板块</h3>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;">
        <div v-for="item in patrolItems" :key="item.name" style="padding:16px;border-radius:8px;text-align:center;" :style="{background: item.color}">
          <i :class="['fas', item.icon]" style="font-size:28px;color:#fff;margin-bottom:8px;"></i>
          <p style="font-size:13px;font-weight:600;color:#fff;">{{ item.name }}</p>
        </div>
      </div>
    </div>

    <!-- 喊话器与载荷控制 -->
    <div v-if="activeTab === 'speaker'">
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
        <!-- 喊话器控制 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;"><i class="fas fa-bullhorn" style="color:#1890ff;margin-right:6px;"></i>喊话器控制</h3>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">选择设备</label>
            <select v-model="selectedSpeakerDevice" style="width:100%;padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
              <option value="">请选择设备</option>
              <option v-for="d in devices" :key="d.deviceSn" :value="d.deviceSn">
                {{ d.deviceName || d.nickname }} ({{ d.deviceSn }})
              </option>
            </select>
          </div>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">音量: {{ speakerVolume }}%</label>
            <input type="range" v-model.number="speakerVolume" min="0" max="100" style="width:100%;" @change="setSpeakerVolume" />
          </div>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">音频文件</label>
            <div style="max-height:120px;overflow-y:auto;border:1px solid #e5e7eb;border-radius:4px;">
              <div v-for="file in speakerFiles" :key="file.id" style="display:flex;align-items:center;justify-content:space-between;padding:6px 10px;border-bottom:1px solid #f3f4f6;">
                <span style="font-size:12px;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ file.name || file.fileName || file.id }}</span>
                <div style="display:flex;gap:4px;">
                  <button @click="playSpeaker(file.id)" style="padding:2px 8px;border:1px solid #52c41a;border-radius:4px;background:#fff;color:#52c41a;font-size:11px;cursor:pointer;">播放</button>
                  <button @click="stopSpeakerAction" style="padding:2px 8px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;font-size:11px;cursor:pointer;">停止</button>
                </div>
              </div>
              <p v-if="!speakerFiles.length" style="text-align:center;padding:10px;font-size:12px;color:#9ca3af;">暂无音频文件</p>
            </div>
          </div>
        </div>

        <!-- 相机控制 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;"><i class="fas fa-camera" style="color:#1890ff;margin-right:6px;"></i>相机载荷控制</h3>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">选择设备</label>
            <select v-model="selectedCameraDevice" style="width:100%;padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
              <option value="">请选择设备</option>
              <option v-for="d in devices" :key="d.deviceSn" :value="d.deviceSn">
                {{ d.deviceName || d.nickname }} ({{ d.deviceSn }})
              </option>
            </select>
          </div>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">相机模式</label>
            <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:8px;">
              <button @click="switchCameraMode(0)" :style="{padding:'8px',border:'1px solid ' + (cameraMode === 0 ? '#1890ff' : '#d1d5db'),borderRadius:'4px',background: cameraMode === 0 ? '#e6f7ff' : '#fff',fontSize:'12px',cursor:'pointer'}">
                <i class="fas fa-camera"></i> 拍照
              </button>
              <button @click="switchCameraMode(1)" :style="{padding:'8px',border:'1px solid ' + (cameraMode === 1 ? '#1890ff' : '#d1d5db'),borderRadius:'4px',background: cameraMode === 1 ? '#e6f7ff' : '#fff',fontSize:'12px',cursor:'pointer'}">
                <i class="fas fa-video"></i> 录像
              </button>
              <button @click="switchCameraMode(2)" :style="{padding:'8px',border:'1px solid ' + (cameraMode === 2 ? '#1890ff' : '#d1d5db'),borderRadius:'4px',background: cameraMode === 2 ? '#e6f7ff' : '#fff',fontSize:'12px',cursor:'pointer'}">
                <i class="fas fa-moon"></i> 低光
              </button>
              <button @click="switchCameraMode(3)" :style="{padding:'8px',border:'1px solid ' + (cameraMode === 3 ? '#1890ff' : '#d1d5db'),borderRadius:'4px',background: cameraMode === 3 ? '#e6f7ff' : '#fff',fontSize:'12px',cursor:'pointer'}">
                <i class="fas fa-expand"></i> 全景
              </button>
            </div>
          </div>
          <div style="margin-bottom:12px;">
            <label style="font-size:12px;color:#6b7280;display:block;margin-bottom:4px;">录像控制</label>
            <div style="display:flex;gap:8px;">
              <button @click="startVideoRecord" :disabled="isRecording" style="flex:1;padding:8px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">
                <i class="fas fa-circle"></i> 开始
              </button>
              <button @click="stopVideoRecord" :disabled="!isRecording" style="flex:1;padding:8px;border:1px solid #6b7280;border-radius:4px;background:#fff;color:#6b7280;font-size:12px;cursor:pointer;">
                <i class="fas fa-stop"></i> 停止
              </button>
            </div>
            <p v-if="isRecording" style="text-align:center;margin-top:8px;font-size:11px;color:#ff4d4f;">
              <span style="width:6px;height:6px;background:#ff4d4f;border-radius:50%;display:inline-block;animation:pulse 1s infinite;margin-right:4px;"></span>
              正在录像中...
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- 媒体中心（预留） -->
    <div v-if="activeTab === 'media'" class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;"><i class="fas fa-photo-video" style="color:#1890ff;margin-right:6px;"></i>媒体中心</h3>
      <table class="table">
        <thead><tr><th>文件名</th><th>类型</th><th>无人机</th><th>拍摄负载</th><th>创建时间</th></tr></thead>
        <tbody>
          <tr v-for="m in mediaList" :key="m.id || m.fileName">
            <td>{{ m.fileName || m.name || '-' }}</td>
            <td><span :class="['tag', m.fileType === 'video' ? 'tag-blue' : 'tag-green']">{{ m.fileType === 'video' ? '视频' : '图片' }}</span></td>
            <td style="font-size:12px;">{{ m.droneSn || m.device_sn || '-' }}</td>
            <td style="font-size:12px;">{{ m.payloadName || m.payload || '-' }}</td>
            <td style="font-size:12px;">{{ m.createTime || m.created_at || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!mediaList.length" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-folder-open" style="font-size:36px;margin-bottom:12px;color:#d1d5db;"></i>
        <br />媒体数据接口待对接，当前为空
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import http, { getJobs, createJob, pauseResumeJob, returnHome, getSpeakerFiles, playSpeaker as playSpeakerAction, stopSpeaker, setSpeakerVolume as setSpeakerVolumeAction, switchCameraMode as switchCameraModeAction, startRecording, stopRecording } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getEventTypeName } from '../utils/eventTypes'

const tabs = [
  { key: 'devices', label: '无人机设备' },
  { key: 'jobs', label: '巡检任务' },
  { key: 'waylines', label: '航线管理' },
  { key: 'video', label: '视频监控' },
  { key: 'alerts', label: 'AI告警' },
  { key: 'patrol', label: '巡检板块' },
  { key: 'speaker', label: '喊话控制' },
  { key: 'media', label: '媒体中心' },
]
const activeTab = ref('devices')
const overview = ref<any>({})
const devices = ref<any[]>([])
const jobs = ref<any[]>([])
const waylines = ref<any[]>([])
const cameras = ref<any[]>([])
const externalSources = ref<any[]>([])
const aiAlerts = ref<any[]>([])
const selectedDrone = ref<any>(null)
const wsUrl = ref('')
const streamType = ref('')
const wsConnected = ref(false)
const realtimeData = ref<any>({})
let droneMap: any = null
let mapMarkers: any[] = []
let hlsPlayer: any = null
let wsSocket: WebSocket | null = null

// 任务管理
const showCreateJob = ref(false)
const jobStatusFilter = ref<number | undefined>(undefined)
const newJob = ref<{ dockSn: string; fileId: string }>({ dockSn: '', fileId: '' })

// 喊话器/相机控制
const selectedSpeakerDevice = ref('')
const selectedCameraDevice = ref('')
const speakerVolume = ref(50)
const cameraMode = ref(0)
const isRecording = ref(false)
const speakerFiles = ref<any[]>([])

// 媒体中心（预留）
const mediaList = ref<any[]>([])

const patrolItems = [
  { name: '环境卫生监测', icon: 'fa-trash', color: '#52c41a' },
  { name: '消防安全预警', icon: 'fa-fire', color: '#ff4d4f' },
  { name: '防洪防汛调度', icon: 'fa-water', color: '#1890ff' },
  { name: '占道经营识别', icon: 'fa-store', color: '#faad14' },
  { name: '井盖缺失排查', icon: 'fa-exclamation-circle', color: '#722ed1' },
  { name: '违章建筑监管', icon: 'fa-building', color: '#eb2f96' },
  { name: '车辆轨迹追踪', icon: 'fa-car', color: '#13c2c2' },
  { name: '公共安全', icon: 'fa-shield-alt', color: '#fa541c' },
  { name: '人员轨迹追踪', icon: 'fa-walking', color: '#2f54eb' },
]

function isOnline(d: any) {
  return d.boundStatus === true || 'ONLINE' === d.deviceStatus || 'online' === String(d.status) || d.online === true
}

function jobStatusLabel(j: any) {
  const s = j.status
  if (s === 0) return '待执行'
  if (s === 1) return '执行中'
  if (s === 2) return '已完成'
  if (s === 3) return '已完成'
  if (s === 4) return '已取消'
  if (s === 5) return '执行失败'
  return s?.toString() || '未知'
}

function jobStatusClass(j: any) {
  const s = j.status
  if (s === 1) return 'tag-green'
  if (s === 2 || s === 3) return 'tag-blue'
  if (s === 5) return 'tag-red'
  if (s === 4) return 'tag-orange'
  return 'tag-orange'
}

// 任务管理函数
async function createNewJob() {
  if (!newJob.value.dockSn || !newJob.value.fileId) return
  try {
    await createJob(newJob.value)
    showCreateJob.value = false
    newJob.value = { dockSn: '', fileId: '' }
    loadData()
  } catch (e) {
    console.error('创建任务失败:', e)
  }
}

async function pauseJob(jobId: string) {
  try {
    await pauseResumeJob(jobId, 0)
    loadData()
  } catch (e) { console.error('挂起任务失败:', e) }
}

async function resumeJob(jobId: string) {
  try {
    await pauseResumeJob(jobId, 1)
    loadData()
  } catch (e) { console.error('恢复任务失败:', e) }
}

async function returnHomeJob(dockSn: string) {
  try {
    await returnHome(dockSn)
    loadData()
  } catch (e) { console.error('返航失败:', e) }
}

// 喊话器控制
async function loadSpeakerFiles() {
  try {
    const res: any = await getSpeakerFiles()
    speakerFiles.value = res?.items || res?.data?.items || []
  } catch (e) { console.error('加载音频失败:', e) }
}

async function playSpeaker(fileId: number) {
  if (!selectedSpeakerDevice.value) return
  try { await playSpeakerAction(selectedSpeakerDevice.value, fileId) } catch (e) { console.error('播放失败:', e) }
}

async function stopSpeakerAction() {
  if (!selectedSpeakerDevice.value) return
  try { await stopSpeaker(selectedSpeakerDevice.value) } catch (e) { console.error('停止失败:', e) }
}

async function setSpeakerVolume() {
  if (!selectedSpeakerDevice.value) return
  try { await setSpeakerVolumeAction(selectedSpeakerDevice.value, speakerVolume.value) } catch (e) { console.error('设置音量失败:', e) }
}

// 相机控制
async function switchCameraMode(mode: number) {
  if (!selectedCameraDevice.value) return
  try {
    await switchCameraModeAction(selectedCameraDevice.value, mode)
    cameraMode.value = mode
  } catch (e) { console.error('切换模式失败:', e) }
}

async function startVideoRecord() {
  if (!selectedCameraDevice.value) return
  try {
    await startRecording(selectedCameraDevice.value)
    isRecording.value = true
  } catch (e) { console.error('开始录像失败:', e) }
}

async function stopVideoRecord() {
  if (!selectedCameraDevice.value) return
  try {
    await stopRecording(selectedCameraDevice.value)
    isRecording.value = false
  } catch (e) { console.error('停止录像失败:', e) }
}

async function loadData() {
  try {
    const [overviewRes, devicesRes, jobsRes, waylinesRes, camerasRes, sourcesRes, alertsRes] = await Promise.all([
      http.get('/drone/dashboard/overview').catch(() => ({})),
      http.get('/drone/dashboard/devices').catch(() => []),
      getJobs(jobStatusFilter.value !== undefined ? { status: jobStatusFilter.value } : {}).catch(() => []),
      http.get('/drone/dashboard/waylines').catch(() => []),
      http.get('/video/cameras').catch(() => []),
      http.get('/video/external-sources').catch(() => []),
      http.get('/drone/dashboard/ai-alerts').catch(() => []),
    ])
    overview.value = overviewRes || {}
    // 如果外部API无数据，显示示例数据
    devices.value = devicesRes && devicesRes.length > 0 ? devicesRes : getSampleDevices()
    jobs.value = jobsRes && jobsRes.length > 0 ? jobsRes : getSampleJobs()
    waylines.value = waylinesRes && waylinesRes.length > 0 ? waylinesRes : getSampleWaylines()
    cameras.value = camerasRes || []
    externalSources.value = sourcesRes || []
    aiAlerts.value = alertsRes || []
  } catch (e) {
    console.error(e)
  }
}

function getSampleDevices() {
  return [
    { id: 1, deviceName: '机场3', deviceSn: '8UUDM6400AY6S4', nickname: '佛堂', boundStatus: true, deviceType: 2, longitude: 120.06589, latitude: 29.2016,
      videoPlayUrlWebRtc: [{ videoList: [{ playUrl: 'https://drone.kfktec.cn:9085/index/api/whep?app=live&stream=8UUDM6400AY6S4_165-0-7_normal-0' }] }],
      videoPlayUrlInner: [{ videoList: [{ playUrl: 'ws://8.156.93.151:11080/index/api/whip.live.flv?originTypeStr=rtmp_push&sign=41db35390ddad33f83944f44b8b75ded' }] }],
      videoUrl: [{ videoList: [{ playUrl: 'https://drone.kfktec.cn:9085/live/8UUDM6400AY6S4_165-0-7_normal-0/hls.m3u8' }] }] },
    { id: 2, deviceName: '4TD', deviceSn: '1581F8HGD249Q0010233', nickname: '佛堂4TD', boundStatus: true, deviceType: 3, longitude: 120.06600, latitude: 29.2020,
      videoPlayUrlWebRtc: [{ videoList: [{ playUrl: 'https://drone.kfktec.cn:9085/index/api/whep?app=live&stream=1581F8HGD249Q0010233_99-0-0_normal-0' }] }],
      videoUrl: [{ videoList: [{ playUrl: 'https://drone.kfktec.cn:9085/live/1581F8HGD249Q0010233_99-0-0_normal-0/hls.m3u8' }] }] },
  ]
}

function getSampleJobs() {
  return [
    { job_id: '1', job_name: '722航线测试', file_name: '道路损伤巡查航线', dock_name: '佛堂', execute_time: '2026-07-22 15:19', completed_time: '2026-07-22 15:27', media_count: 2, uploaded_count: 2, status: 3 },
    { job_id: '2', job_name: 'test', file_name: '测试航线', dock_name: '佛堂', execute_time: '2026-07-16 16:37', completed_time: '2026-07-16 16:39', media_count: 0, uploaded_count: 0, status: 5 },
  ]
}

function getSampleWaylines() {
  return [
    { id: 1, waylineName: 'A区巡检航线', waylineType: '点状航线', updateTime: '2026-07-23' },
    { id: 2, waylineName: 'B区巡检航线', waylineType: '点状航线', updateTime: '2026-07-23' },
    { id: 3, waylineName: '全域巡检航线', waylineType: '面状航线', updateTime: '2026-07-22' },
  ]
}

/**
 * HLS 实时直播播放器 - 通过后端代理
 * 前端连接固定代理地址，后端自动处理签名修复和转发
 */
async function selectDrone(d: any) {
  selectedDrone.value = d
  wsUrl.value = ''
  streamType.value = ''

  // 销毁旧播放器
  _destroyPlayer()

  const deviceSn = d.deviceSn || d.childSn
  if (!deviceSn) return

  // 使用 HLS 流（通过后端代理，自动修复签名URL）
  wsUrl.value = `/api/drone/stream/proxy/${deviceSn}/hls.m3u8`
  streamType.value = 'HLS 实时直播'

  await nextTick()
  const video = document.getElementById('droneVideo') as HTMLVideoElement
  if (!video) return

  // 销毁旧播放器
  if (hlsPlayer) {
    try { hlsPlayer.destroy() } catch (e) {}
    hlsPlayer = null
  }

  const Hls = (await import('hls.js')).default
  hlsPlayer = new Hls({
    enableWorker: true,
    lowLatencyMode: true,
    // 流畅直播配置
    backBufferLength: 90,
    maxBufferLength: 30,
    maxMaxBufferLength: 60,
    liveSyncDurationCount: 2,
    liveMaxLatencyDurationCount: 5,
    // 超时配置
    manifestLoadingTimeOut: 10000,
    manifestLoadingMaxRetry: 3,
    levelLoadingTimeOut: 10000,
    levelLoadingMaxRetry: 3,
    fragLoadingTimeOut: 20000,
    fragLoadingMaxRetry: 6
  })
  hlsPlayer.loadSource(wsUrl.value)
  hlsPlayer.attachMedia(video)
  hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => {
    video.play().catch(() => {})
    // 直播模式：始终跳到最新位置
    if (video.buffered.length > 0) {
      video.currentTime = video.buffered.end(video.buffered.length - 1)
    }
  })
  hlsPlayer.on(Hls.Events.ERROR, (_: any, data: any) => {
    if (data.fatal) {
      console.warn('HLS致命错误:', data.details, '- 2秒后重试')
      setTimeout(() => {
        if (hlsPlayer && wsUrl.value) {
          hlsPlayer.loadSource(wsUrl.value + '?t=' + Date.now())
          hlsPlayer.attachMedia(document.getElementById('droneVideo') as HTMLVideoElement)
        }
      }, 2000)
    }
  })
  // 定期追最新画面（防止延迟过大）
  setInterval(() => {
    if (video.buffered.length > 0 && !video.paused) {
      const latest = video.buffered.end(video.buffered.length - 1)
      // 只在大延迟时跳转，避免频繁跳动
      if (latest - video.currentTime > 5) {
        video.currentTime = latest
      }
    }
  }, 2000)

  // 连接 WebSocket 实时数据（仅视频成功时）
  if (wsUrl.value && wsUrl.value !== 'pending') {
    _connectWebSocket(deviceSn)
  }

  // 地图聚焦到该设备
  if (droneMap && d.longitude && d.latitude) {
    droneMap.setZoomAndCenter(16, [d.longitude, d.latitude])
  }
}

function _destroyPlayer() {
  if (flvPlayer) {
    try { flvPlayer.destroy() } catch (e) {}
    flvPlayer = null
  }
  if (hlsPlayer) {
    try { hlsPlayer.destroy() } catch (e) {}
    hlsPlayer = null
  }
  _disconnectWebSocket()
}

// WebSocket 实时数据连接
function _connectWebSocket(deviceSn: string) {
  _disconnectWebSocket()
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  const token = session?.token
  if (!token) return

  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${wsProtocol}//${window.location.host}/api/ws/drone?token=${encodeURIComponent(token)}&deviceSn=${encodeURIComponent(deviceSn)}`

  wsSocket = new WebSocket(wsUrl)

  wsSocket.onopen = () => {
    wsConnected.value = true
    console.log('WebSocket 已连接')
  }

  wsSocket.onmessage = (event: MessageEvent) => {
    console.log('WebSocket 收到消息:', event.data.substring(0, 200))
    try {
      const msg = JSON.parse(event.data)
      if (msg.type === 'connected') {
        console.log('WebSocket 连接确认:', msg.deviceSn)
        return
      }
      // 处理遥测数据
      if (msg.type === 'device_osd' || msg.type === 'dock_osd' || msg.biz_code === 'device_osd' || msg.biz_code === 'dock_osd') {
        const data = msg.data || msg
        realtimeData.value = { ...realtimeData.value, ...data }
        // 映射字段
        if (data.battery) realtimeData.value.battery = typeof data.battery === 'object' ? data.battery.capacity_percent : data.battery
        if (data.elevation) realtimeData.value.height = data.elevation
        if (data.horizontal_speed) realtimeData.value.speed = data.horizontal_speed
        if (data.longitude) realtimeData.value.longitude = data.longitude
        if (data.latitude) realtimeData.value.latitude = data.latitude
        if (data.modeCode !== undefined) {
          const modes: any = { 0: '待机', 4: '工作中', 99: '离线' }
          realtimeData.value.mode = modes[data.modeCode] || `模式${data.modeCode}`
        }
        if (data.temperature !== undefined) realtimeData.value.temperature = data.temperature
        if (data.wind_speed !== undefined) realtimeData.value.windSpeed = data.wind_speed
        if (data.rainfall !== undefined) realtimeData.value.rainfall = data.rainfall
        console.log('实时数据更新:', realtimeData.value)
      }
    } catch (e) {
      console.warn('WebSocket 消息解析失败:', e)
    }
  }

  wsSocket.onclose = () => {
    wsConnected.value = false
    // 5秒后重连
    setTimeout(() => {
      if (selectedDrone.value) _connectWebSocket(deviceSn)
    }, 5000)
  }

  wsSocket.onerror = () => {
    wsConnected.value = false
  }
}

function _disconnectWebSocket() {
  if (wsSocket) {
    try { wsSocket.close() } catch (e) {}
    wsSocket = null
  }
  wsConnected.value = false
  realtimeData.value = {}
}

// FLV 播放器（通过后端代理转发，避免浏览器直连外部 WebSocket）
let flvPlayer: any = null

async function _initFlvPlayer(_flvUrl: string, deviceSn: string) {
  try {
    const flvjs = (await import('flv.js')).default
    if (!flvjs.isSupported()) {
      console.warn('flv.js 不支持，降级到 HLS')
      return _initHlsPlayer(`/api/drone/stream/proxy/${deviceSn}/hls.m3u8`)
    }

    // 销毁旧播放器
    if (flvPlayer) {
      try { flvPlayer.destroy() } catch (e) {}
      flvPlayer = null
    }
    if (hlsPlayer) {
      try { hlsPlayer.destroy() } catch (e) {}
      hlsPlayer = null
    }

    await nextTick()
    const video = document.getElementById('droneVideo') as HTMLVideoElement
    if (!video) return

    // 使用后端代理 URL
    const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
    const token = session?.token || ''
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const proxyUrl = `${wsProtocol}//${window.location.host}/ws/video/${deviceSn}?token=${encodeURIComponent(token)}`

    flvPlayer = flvjs.createPlayer({
      type: 'flv',
      url: proxyUrl,
      isLive: true,
      hasAudio: true,
      hasVideo: true
    }, {
      enableWorker: false,
      enableStashBuffer: false,
      stashInitialSize: 64,
      lazyLoad: false
    })

    flvPlayer.attachMediaElement(video)
    flvPlayer.load()
    flvPlayer.play().catch(() => {})

    flvPlayer.on(flvjs.Events.ERROR, (e: any) => {
      console.warn('FLV错误:', e)
    })
  } catch (e) {
    console.error('FLV播放器初始化失败:', e)
  }
}

async function _initHlsPlayer(url: string) {
  if (!url) return
  await nextTick()
  const video = document.getElementById('droneVideo') as HTMLVideoElement
  if (!video) return

  const Hls = (await import('hls.js')).default
  hlsPlayer = new Hls({
    enableWorker: true,
    lowLatencyMode: true,
    backBufferLength: 2,
    maxBufferLength: 4,
    liveSyncDurationCount: 1
  })
  hlsPlayer.loadSource(url)
  hlsPlayer.attachMedia(video)
  hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => { video.play().catch(() => {}) })
  hlsPlayer.on(Hls.Events.ERROR, (_: any, data: any) => {
    if (data.fatal) console.warn('HLS错误:', data.details)
  })
}

async function initDroneMap() {
  if (activeTab.value !== 'video') return
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.Marker', 'AMap.InfoWindow']
    })
    droneMap = new AMap.Map('droneMap', { zoom: 13, center: [120.06589, 29.2016], mapStyle: 'amap://styles/normal' })

    // 添加设备标记
    devices.value.forEach((d: any) => {
      if (d.longitude && d.latitude) {
        const marker = new AMap.Marker({
          position: [d.longitude, d.latitude],
          title: d.deviceName || d.nickname,
          map: droneMap,
          label: { content: d.deviceName || d.nickname, direction: 'top' }
        })
        marker.on('click', () => selectDrone(d))
        mapMarkers.push(marker)
      }
    })
  } catch (e) {
    console.error('地图初始化失败:', e)
  }
}

watch(activeTab, (tab) => {
  if (tab === 'video') {
    setTimeout(initDroneMap, 100)
  }
  if (tab === 'speaker') {
    loadSpeakerFiles()
    if (devices.value.length) {
      if (!selectedSpeakerDevice.value) selectedSpeakerDevice.value = devices.value[0].deviceSn
      if (!selectedCameraDevice.value) selectedCameraDevice.value = devices.value[0].deviceSn
    }
  }
})

onMounted(() => {
  loadData()
  // 如果默认是视频标签，初始化地图
  if (activeTab.value === 'video') {
    setTimeout(initDroneMap, 100)
  }
})

onUnmounted(() => {
  _destroyPlayer()
  // 清理地图
  if (droneMap) {
    try { droneMap.destroy() } catch (e) {}
    droneMap = null
  }
  mapMarkers = []
})
</script>

<style scoped>
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}
</style>
