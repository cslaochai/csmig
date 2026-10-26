import { createRouter, createWebHistory } from 'vue-router'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/login/LoginView.vue'),
        meta: { public: true }
    },
    {
        path: '/',
        component: () => import('@/views/Layout.vue'),
        children: [
            { path: '', redirect: '/monitor' },
            // ---- 运行监控 ----
            { path: 'monitor', name: 'Monitor', component: () => import('@/views/Monitor.vue'), meta: { title: '接口监控', icon: 'Odometer' } },
            { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '实时交易看板', icon: 'TrendCharts' } },
            { path: 'alerts', name: 'Alerts', component: () => import('@/views/Alerts.vue'), meta: { title: '异常告警', icon: 'Bell' } },
            // ---- 交易管理 ----
            { path: 'logs', name: 'Logs', component: () => import('@/views/Logs.vue'), meta: { title: '交易流水', icon: 'List' } },
            { path: 'compensation', name: 'Compensation', component: () => import('@/views/Compensation.vue'), meta: { title: '失败补偿', icon: 'RefreshLeft' } },
            // ---- 对账结算 ----
            { path: 'recon', name: 'Recon', component: () => import('@/views/Recon.vue'), meta: { title: '对账报表', icon: 'Tickets' } },
            { path: 'summary', name: 'Summary', component: () => import('@/views/Summary.vue'), meta: { title: '结算汇总', icon: 'DataLine' } },
            // ---- 字典目录 ----
            { path: 'dict', name: 'Dict', component: () => import('@/views/Dict.vue'), meta: { title: '医保目录缓存', icon: 'Files' } },
            { path: 'person', name: 'Person', component: () => import('@/views/Person.vue'), meta: { title: '参保人信息', icon: 'User' } },
            // ---- 系统管理 ----
            { path: 'config', name: 'Config', component: () => import('@/views/Config.vue'), meta: { title: '基础配置', icon: 'Setting' } },
            { path: 'cert', name: 'Cert', component: () => import('@/views/Cert.vue'), meta: { title: '密钥与证书', icon: 'Key' } }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
