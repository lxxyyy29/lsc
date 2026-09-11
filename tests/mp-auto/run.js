/**
 * 微信小程序全链路 UI 自动化驱动（miniprogram-automator）
 * 前置：微信开发者工具「设置→安全设置→服务端口」开启
 * 运行：node run.js [step]
 *   step=1 启动+首页快照；之后按需扩展登录/上报等流程
 */
const automator = require('miniprogram-automator')

const PROJECT = 'D:/kfkprojuct/h5/dist/build/mp-weixin'
const CLI = 'D:/微信web开发者工具/cli.bat'

async function dumpTexts(page, label) {
  const texts = await page.$$('text,button')
  const out = []
  for (const t of texts) {
    const txt = await t.text()
    if (txt && txt.trim()) out.push(txt.trim())
  }
  console.log(`\n[${label}] route=${page.path} 元素(${out.length}):`)
  console.log('  ' + out.slice(0, 60).join(' | '))
  return texts
}

async function waitPage(mp, pathPart, timeoutMs = 20000) {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    const p = await mp.currentPage()
    if (p.path.includes(pathPart)) return p
    await new Promise(r => setTimeout(r, 1000))
  }
  throw new Error(`等待页面 ${pathPart} 超时`)
}

async function main() {
  const step = process.argv[2] || '1'
  console.log('[auto] 启动微信小程序 ...')
  const mp = await automator.launch({ cliPath: CLI, projectPath: PROJECT })
  console.log('[auto] 已连接开发者工具')

  // 首页应为角色选择页
  const home = await waitPage(mp, 'role-select', 30000)
  await dumpTexts(home, '首页-角色选择')

  if (step === '1') {
    // 第一阶段：只确认首页结构与网格员登录入口
    await mp.close()
    console.log('[auto] step1 完成')
    return
  }
  await mp.close()
}

main().catch(e => { console.error('[auto] FAIL:', e.message || e); process.exit(1) })
