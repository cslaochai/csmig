import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { installRouteGuard } from './router/guard'
import { ElMessage } from 'element-plus'
import './style.css'

const app = createApp(App)
for (const [k, v] of Object.entries(ElementPlusIconsVue)) app.component(k, v)
app.use(ElementPlus).use(router).mount('#app')

// 必须在 app.use(router) 之后注册 —— 守卫依赖路由实例已就绪
installRouteGuard(router, (msg) => ElMessage.warning(msg))
