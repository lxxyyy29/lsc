import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import LoginView from './LoginView.vue'

const loginWeb = vi.fn()

vi.mock('../../api/auth', () => ({
  loginWeb: (...args: unknown[]) => loginWeb(...args)
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/login',
        component: LoginView
      },
      {
        path: '/dashboard',
        component: {
          template: '<div>dashboard</div>'
        }
      },
      {
        path: '/events',
        component: {
          template: '<div>events</div>'
        }
      }
    ]
  })
}

async function renderAt(path = '/login') {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(LoginView, {
    global: {
      plugins: [router]
    }
  })

  return router
}

afterEach(() => {
  cleanup()
})

beforeEach(() => {
  loginWeb.mockReset()
})

describe('LoginView', () => {
  it('renders the login surface with platform title and credential form', async () => {
    await renderAt('/login')

    expect(screen.getByText('居里智能低空巡检综合监管平台')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '欢迎登录' })).toBeTruthy()
    expect(screen.getByLabelText('账号')).toBeTruthy()
    expect(screen.getByLabelText('密码')).toBeTruthy()
    expect(screen.getByRole('button', { name: '登录平台' })).toBeTruthy()
  })

  it('still submits through loginWeb without client-side required-field blocking', async () => {
    loginWeb.mockRejectedValue(new Error('服务端返回的错误'))
    await renderAt('/login')

    await fireEvent.click(screen.getByRole('button', { name: '登录平台' }))

    await waitFor(() => {
      expect(loginWeb).toHaveBeenCalledWith({
        account: '',
        password: ''
      })
    })
    expect(screen.queryByText('请输入账号和密码')).toBeNull()
    expect((await screen.findByRole('alert')).textContent).toContain('服务端返回的错误')
  })

  it('submits credentials through loginWeb and redirects to the requested route', async () => {
    loginWeb.mockResolvedValue({ token: 'token-1' })
    const router = await renderAt('/login?redirect=/events')

    await fireEvent.update(screen.getByLabelText('账号'), 'admin')
    await fireEvent.update(screen.getByLabelText('密码'), 'secret')
    await fireEvent.click(screen.getByRole('button', { name: '登录平台' }))

    await waitFor(() => {
      expect(loginWeb).toHaveBeenCalledWith({
        account: 'admin',
        password: 'secret'
      })
      expect(router.currentRoute.value.fullPath).toBe('/events')
    })
  })
})
