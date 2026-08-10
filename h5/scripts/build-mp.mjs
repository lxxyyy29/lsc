#!/usr/bin/env node
/**
 * 微信小程序构建脚本
 *
 * pages.json 不支持条件编译，因此：
 * 1. 备份当前 pages.json（工作人员端 + 居民端全量）
 * 2. 生成小程序版 pages.json（仅居民端 10 页，首个为 login，配置原生 tabBar）
 * 3. 执行 `uni build -p mp-weixin`
 * 4. 无论成败，finally 恢复原始 pages.json
 */
import { readFileSync, writeFileSync, existsSync, rmSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import { spawnSync } from 'node:child_process'

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const pagesJsonPath = resolve(rootDir, 'pages.json')
const backupPath = resolve(rootDir, 'pages.json.h5-backup')

const RESIDENT_PAGES = [
  { path: 'pages/resident/login/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/register/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/report/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/history/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/services/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/activities/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/repairs/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/policies/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/points/index', style: { navigationStyle: 'custom' } },
  { path: 'pages/resident/mine/index', style: { navigationStyle: 'custom' } }
]

/** 小程序原生 tabBar：仅文字（iconPath 可选），浅色主题与居民端页面一致 */
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
    pages: RESIDENT_PAGES,
    tabBar: MP_TAB_BAR,
    globalStyle: {
      navigationBarTextStyle: 'white',
      navigationBarTitleText: '拔蛟窝智慧网格',
      navigationBarBackgroundColor: '#1890ff',
      backgroundColor: '#f5f7fa'
    }
  }

  writeFileSync(pagesJsonPath, JSON.stringify(mpPagesJson, null, 2) + '\n', 'utf-8')
  console.log(`[build-mp] 已生成小程序版 pages.json（${RESIDENT_PAGES.length} 页）`)

  try {
    const result = spawnSync(
      'npx',
      ['cross-env', 'UNI_INPUT_DIR=.', 'uni', 'build', '-p', 'mp-weixin'],
      { cwd: rootDir, stdio: 'inherit', shell: process.platform === 'win32' }
    )
    if (result.status !== 0) {
      process.exitCode = result.status ?? 1
    }
  } finally {
    writeFileSync(pagesJsonPath, original, 'utf-8')
    rmSync(backupPath, { force: true })
    console.log('[build-mp] 已恢复原始 pages.json')
  }
}

main()
