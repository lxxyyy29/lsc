// 全局轻量 toast 提示：替代原生 alert()，不阻塞页面交互
// 用法：showMessage('保存成功') 自动识别类型，或 showMessage('xx', 'error', 5000) 显式指定
export type MessageType = 'success' | 'error' | 'warning' | 'info'

let container: HTMLDivElement | null = null

const TYPE_STYLE: Record<MessageType, { bg: string; border: string; icon: string }> = {
  success: { bg: '#f6ffed', border: '#b7eb8f', icon: '✅' },
  error: { bg: '#fff2f0', border: '#ffccc7', icon: '❌' },
  warning: { bg: '#fffbe6', border: '#ffe58f', icon: '⚠️' },
  info: { bg: '#e6f4ff', border: '#91caff', icon: 'ℹ️' },
}

function ensureContainer(): HTMLDivElement {
  if (container && document.body.contains(container)) return container
  const el = document.createElement('div')
  el.style.cssText = 'position:fixed;top:64px;left:50%;transform:translateX(-50%);z-index:10001;display:flex;flex-direction:column;align-items:center;gap:8px;pointer-events:none;'
  document.body.appendChild(el)
  container = el
  return el
}

// 按文案内容自动识别提示类型：后端错误信息/成功提示/表单校验各归其类
function detectType(text: string): MessageType {
  if (/成功|已生成|已发送|已重置|已通过|已驳回|已拒绝|已取消/.test(text)) return 'success'
  if (/失败|错误|过期|无法|拒绝/.test(text)) return 'error'
  if (/请先|请输入|请填写|请勾选|请选择|请重新|必须|不可用|没有可|已存在/.test(text)) return 'warning'
  return 'info'
}

export function showMessage(text: string, typeOrDuration?: MessageType | number, duration?: number) {
  const type: MessageType = typeof typeOrDuration === 'string' ? typeOrDuration : detectType(text)
  const stay = typeof typeOrDuration === 'number' ? typeOrDuration : (duration ?? (type === 'error' ? 5000 : 3000))
  const box = ensureContainer()
  const item = document.createElement('div')
  const s = TYPE_STYLE[type]
  item.style.cssText = `pointer-events:auto;max-width:480px;padding:10px 16px;border-radius:6px;background:${s.bg};border:1px solid ${s.border};color:#374151;font-size:13px;line-height:1.6;box-shadow:0 4px 12px rgba(0,0,0,0.08);white-space:pre-line;cursor:pointer;opacity:0;transform:translateY(-8px);transition:opacity .2s,transform .2s;`
  item.textContent = `${s.icon} ${text}`
  item.title = '点击关闭'
  box.appendChild(item)
  requestAnimationFrame(() => { item.style.opacity = '1'; item.style.transform = 'translateY(0)' })
  const close = () => {
    item.style.opacity = '0'
    item.style.transform = 'translateY(-8px)'
    setTimeout(() => item.remove(), 200)
  }
  item.addEventListener('click', close)
  setTimeout(close, stay)
}
