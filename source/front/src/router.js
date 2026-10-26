import { createRouter, createWebHistory } from 'vue-router'
import Monitor from './views/Monitor.vue'
import Logs from './views/Logs.vue'
import Config from './views/Config.vue'

export default createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/monitor' },
    { path: '/monitor', component: Monitor, meta: { title: '接口监控' } },
    { path: '/logs', component: Logs, meta: { title: '交易流水' } },
    { path: '/config', component: Config, meta: { title: '配置 & 对账' } }
  ]
})
