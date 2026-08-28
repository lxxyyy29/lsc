// 系统级弹窗工具：Promise 风格的确认框与输入框，替代浏览器原生 confirm()/prompt()
// 风格与系统 modal-overlay/modal-box 一致，z-index 10001 确保嵌套场景位于最上层
import { createApp, h, reactive } from 'vue'

export interface ConfirmOptions {
  title?: string
  message: string
  okText?: string
  danger?: boolean
}

export interface PromptOptions {
  title?: string
  message?: string
  placeholder?: string
  required?: boolean
  rows?: number
}

function mountOverlay(renderBox: () => any, onClose: (result: any) => void) {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const state = reactive({ visible: true })
  const finish = (result: any) => {
    if (!state.visible) return
    state.visible = false
    app.unmount()
    container.remove()
    onClose(result)
  }
  const app = createApp({
    render() {
      if (!state.visible) return null
      return h(
        'div',
        {
          class: 'modal-overlay',
          style: 'z-index:10001;',
          onClick: (e: MouseEvent) => { if (e.target === e.currentTarget) finish(null) },
        },
        [renderBox()],
      )
    },
  })
  app.mount(container)
  return finish
}

/**
 * 确认对话框，返回 Promise<boolean>：确认 true / 取消 false
 */
export function confirmDialog(options: ConfirmOptions): Promise<boolean> {
  return new Promise((resolve) => {
    const finish = mountOverlay(
      () =>
        h('div', { class: 'modal-box', style: 'width:420px;' }, [
          options.title ? h('h3', { style: 'font-size:15px;font-weight:600;margin-bottom:10px;' }, options.title) : null,
          h('p', { style: 'font-size:13px;color:#4b5563;white-space:pre-line;line-height:1.7;' }, options.message),
          h('div', { style: 'display:flex;justify-content:flex-end;gap:8px;margin-top:20px;' }, [
            h('button', { class: 'btn btn-default', onClick: () => finish(false) }, '取消'),
            h('button', { class: `btn ${options.danger ? 'btn-danger' : 'btn-primary'}`, onClick: () => finish(true) }, options.okText || '确定'),
          ]),
        ]),
      (result) => resolve(result ?? false),
    )
  })
}

/**
 * 输入对话框，返回 Promise<string | null>：确认返回输入内容（required 时为空不关闭）/ 取消返回 null
 */
export function promptDialog(options: PromptOptions): Promise<string | null> {
  return new Promise((resolve) => {
    let finishFn: ((r: any) => void) | null = null
    const input = reactive({ value: '', error: '' })
    const confirm = () => {
      if (options.required && !input.value.trim()) {
        input.error = '该项为必填，请填写后再确认'
        return
      }
      finishFn?.(input.value.trim())
    }
    finishFn = mountOverlay(
      () =>
        h('div', { class: 'modal-box', style: 'width:440px;' }, [
          options.title ? h('h3', { style: 'font-size:15px;font-weight:600;margin-bottom:10px;' }, options.title) : null,
          options.message ? h('p', { style: 'font-size:13px;color:#4b5563;white-space:pre-line;line-height:1.7;margin-bottom:10px;' }, options.message) : null,
          h('textarea', {
            value: input.value,
            rows: options.rows || 3,
            placeholder: options.placeholder || '',
            style: 'width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;box-sizing:border-box;',
            onInput: (e: Event) => { input.value = (e.target as HTMLTextAreaElement).value; input.error = '' },
          }),
          input.error ? h('p', { style: 'font-size:12px;color:#ff4d4f;margin-top:6px;' }, input.error) : null,
          h('div', { style: 'display:flex;justify-content:flex-end;gap:8px;margin-top:16px;' }, [
            h('button', { class: 'btn btn-default', onClick: () => finishFn?.(null) }, '取消'),
            h('button', { class: 'btn btn-primary', onClick: confirm }, '确定'),
          ]),
        ]),
      (result) => resolve(result),
    )
  })
}
