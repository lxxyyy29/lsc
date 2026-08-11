#!/usr/bin/env node
/**
 * 微信小程序构建脚本
 *
 * pages.json 不支持条件编译，因此：
 * 1. 备份当前 pages.json（工作人员端 + 居民端全量）
 * 2. 生成小程序版 pages.json（全量 31 页，首个为角色选择页，配置居民端原生 tabBar）
 * 3. 执行 `uni build -p mp-weixin`
 * 4. 无论成败，finally 恢复原始 pages.json
 */
import { readFileSync, writeFileSync, existsSync, rmSync, cpSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const pagesJsonPath = resolve(rootDir, 'pages.json')
const backupPath = resolve(rootDir, 'pages.json.h5-backup')

/** 全量页面：角色选择页为首屏，其后为网格员端 + 居民端全部页面 */
const ALL_PAGES = [
  { path: 'pages/role-select/index', style: { navigationStyle: 'custom' } },

  // ---- 网格员端 ----
  { path: 'pages/login/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/register/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/workbench/index', style: { navigationStyle: 'default', navigationBarTitleText: '工作台' } },
  { path: 'pages/workorder/list', style: { navigationStyle: 'default', navigationBarTitleText: '任务列表' } },
  { path: 'pages/workorder/detail', style: { navigationStyle: 'default', navigationBarTitleText: '工单详情' } },
  { path: 'pages/verify/index', style: { navigationStyle: 'default', navigationBarTitleText: '闭环核查' } },
  { path: 'pages/history/index', style: { navigationStyle: 'default', navigationBarTitleText: '历史记录' } },
  { path: 'pages/mine/index', style: { navigationStyle: 'default', navigationBarTitleText: '个人中心' } },
  { path: 'pages/merchant/list', style: { navigationStyle: 'default', navigationBarTitleText: '商户管理' } },
  { path: 'pages/merchant/detail', style: { navigationStyle: 'default', navigationBarTitleText: '商户详情' } },
  { path: 'pages/vendor/list', style: { navigationStyle: 'default', navigationBarTitleText: '摊贩管理' } },
  { path: 'pages/vendor/detail', style: { navigationStyle: 'default', navigationBarTitleText: '摊贩详情' } },
  { path: 'pages/patrol/checkin', style: { navigationStyle: 'default', navigationBarTitleText: '巡查打卡' } },
  { path: 'pages/patrol/history', style: { navigationStyle: 'default', navigationBarTitleText: '巡查记录' } },
  { path: 'pages/event/report', style: { navigationStyle: 'default', navigationBarTitleText: '事件上报' } },
  { path: 'pages/event/history', style: { navigationStyle: 'default', navigationBarTitleText: '我的上报' } },
  { path: 'pages/map/index', style: { navigationStyle: 'custom', navigationBarTitleText: '移动 GIS' } },
  { path: 'pages/message/index', style: { navigationStyle: 'custom', navigationBarTitleText: '信息互通' } },
  { path: 'pages/volunteer/index', style: { navigationStyle: 'default', navigationBarTitleText: '志愿服务' } },

  // ---- 居民端 ----
  { path: 'pages/resident/login/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/register/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/report/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/history/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/services/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/activities/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/repairs/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/repairs/form', style: { navigationStyle: 'default', navigationBarTitleText: '提交报修' } },
  { path: 'pages/resident/policies/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/points/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/mine/index', style: { navigationStyle: 'custom' } }
]

/** 小程序原生 tabBar：居民端 3 tab（仅文字），浅色主题 */
const MP_TAB_BAR = {
  color: '#9ca3af',
  selectedColor: '#1890ff',
  backgroundColor: '#ffffff',
  borderStyle: 'black',
  list: [
    { pagePath: 'pages/resident/report/index', text: '随手拍' },
    { pagePath: 'pages/resident/services/index', text: '服务' },
    { pagePath: 'pages/resident/mine/index', text: '我的' }
  ]
}

function main() {
  if (!existsSync(pagesJsonPath)) {
    console.error('[build-mp] 未找到 pages.json，请在 h5 工程根目录运行')
    process.exit(1)
  }

  const original = readFileSync(pagesJsonPath, 'utf-8')
  writeFileSync(backupPath, original, 'utf-8')
  console.log('[build-mp] 已备份 pages.json -> pages.json.h5-backup')

  const mpPagesJson = {
    pages: ALL_PAGES,
    tabBar: MP_TAB_BAR,
    globalStyle: {
      navigationBarTextStyle: 'white',
      navigationBarTitleText: '拔蛟窝智慧网格',
      navigationBarBackgroundColor: '#1890ff',
      backgroundColor: '#f5f7fa'
    }
  }

  writeFileSync(pagesJsonPath, JSON.stringify(mpPagesJson, null, 2) + '\n', 'utf-8')
  console.log(`[build-mp] 已生成小程序版 pages.json（${ALL_PAGES.length} 页，首屏角色选择）`)

  try {
    const result = spawnSync(
      'npx',
      ['cross-env', 'UNI_INPUT_DIR=.', 'uni', 'build', '-p', 'mp-weixin'],
      { cwd: rootDir, stdio: 'inherit', shell: process.platform === 'win32' }
    )
    if (result.status !== 0) {
      process.exitCode = result.status ?? 1
    }

    // 拷贝 static 静态资源（uni-app 小程序构建不自动拷贝，marker 图标等需要）
    const staticSrc = resolve(rootDir, 'static')
    const staticDest = resolve(rootDir, 'dist/build/mp-weixin/static')
    if (existsSync(staticSrc) && existsSync(resolve(rootDir, 'dist/build/mp-weixin'))) {
      cpSync(staticSrc, staticDest, { recursive: true })
      console.log('[build-mp] 已拷贝 static 资源')
    }
  } finally {
    writeFileSync(pagesJsonPath, original, 'utf-8')
    rmSync(backupPath, { force: true })
    console.log('[build-mp] 已恢复原始 pages.json')
  }
}

main()
