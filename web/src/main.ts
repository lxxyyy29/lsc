import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import './styles/figma-tokens.css'
import App from './App.vue'
import { fetchCurrentWebUser } from './api/auth'
import { recoverWebSession } from './auth/session'
import { router } from './router'
import { registerDynamicRoutes } from './router/dynamic-routes'

export async function bootstrapApp() {
  const session = await recoverWebSession(fetchCurrentWebUser)
  if (session?.menuTree?.length) {
    registerDynamicRoutes(router, session.menuTree)
  }

  const app = createApp(App)

  app.use(createPinia())
  app.use(router)
  app.use(ElementPlus, { locale: zhCn })
  app.mount('#app')

  return app
}

void bootstrapApp()
