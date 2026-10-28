<template>
  <div class="app-shell">
    <!-- 顶部 Header：与 cslk HIS 同风格（64px 深蓝顶栏 + logo + 面包屑 + 用户区） -->
    <header class="his-header app-header">
      <div class="header-left">
        <router-link to="/" class="brand" title="返回首页">
          <div class="brand-logo-wrap">
            <img alt="长沙市麓康医院" class="brand-logo" src="@/assets/sidebar_logo.png"/>
          </div>
          <span class="brand-name">长沙市麓康医院</span>
        </router-link>
        <nav class="breadcrumb-nav">
          <el-icon class="crumb-icon"><HomeFilled/></el-icon>
          <span class="crumb-label">医保结算前置系统</span>
          <el-icon v-if="currentTitle" class="crumb-sep"><ArrowRight/></el-icon>
          <span v-if="currentTitle" class="crumb-current">{{ currentTitle }}</span>
        </nav>
      </div>
      <div class="header-right">
        <span class="header-user-role">{{ roleLabel }}</span>
        <span class="header-user-name">{{ realName || username }}</span>
        <div class="header-divider"></div>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button class="header-avatar" :title="username">
            {{ (realName || username || '管').charAt(0) }}
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>
                {{ realName || username }} · {{ roleLabel }}
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton/></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 下方：左侧导航 + 主内容 -->
    <div class="app-body">
      <aside class="his-sidebar app-sidebar">
        <nav class="sidebar-nav">
          <div v-for="group in menuGroups" :key="group.title" class="sidebar-group">
            <div class="sidebar-group-title">{{ group.title }}</div>
            <router-link
                v-for="item in group.children"
                :key="item.path"
                :to="item.path"
                :class="['sidebar-item', isActive(item.path) ? 'sidebar-item-active' : '']"
                :title="item.title"
            >
              <el-icon class="sidebar-item-icon"><component :is="item.icon"/></el-icon>
              <span class="sidebar-item-label">{{ item.title }}</span>
            </router-link>
          </div>
        </nav>
        <div class="sidebar-footer">
          <span class="sidebar-footer-text">医保结算前置系统 · v1.0.0</span>
        </div>
      </aside>

      <div class="app-main">
        <main class="app-content">
          <router-view/>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import {computed} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {ArrowRight, Bell, Collection, DataAnalysis, DataLine, Document, Files, HomeFilled, Key, List, Memo, Monitor, Odometer, RefreshLeft, Setting, SwitchButton, Tickets, TrendCharts, User} from '@element-plus/icons-vue'
import {logout as apiLogout} from '@/api/system'

const route = useRoute()
const router = useRouter()

// 一级菜单按业务域分组；结构与国内HIS/医保前置机管理后台一致（监控/交易/对账/字典/系统）
const menuGroups = [
  {
    title: '运行监控',
    children: [
      { path: '/monitor', title: '接口监控', icon: Odometer },
      { path: '/dashboard', title: '实时交易看板', icon: TrendCharts },
      { path: '/alerts', title: '异常告警', icon: Bell }
    ]
  },
  {
    title: '交易管理',
    children: [
      { path: '/logs', title: '交易流水', icon: List },
      { path: '/compensation', title: '失败补偿', icon: RefreshLeft }
    ]
  },
  {
    title: '对账结算',
    children: [
      { path: '/recon', title: '对账报表', icon: Tickets },
      { path: '/summary', title: '结算汇总', icon: DataLine }
    ]
  },
  {
    title: '字典目录',
    children: [
      { path: '/dict', title: '医保目录缓存', icon: Files },
      { path: '/person', title: '参保人信息', icon: User }
    ]
  },
  {
    title: '系统管理',
    children: [
      { path: '/config', title: '基础配置', icon: Setting },
      { path: '/cert', title: '密钥与证书', icon: Key },
      { path: '/audit', title: '操作日志', icon: Memo }
    ]
  }
]

const currentTitle = computed(() => route.meta?.title || '')

const roleLabel = computed(() => {
  const role = localStorage.getItem('currentRole') || 'ADMIN'
  return role === 'ADMIN' ? '系统管理员' : role
})

const realName = computed(() => localStorage.getItem('realName') || '')
const username = computed(() => localStorage.getItem('username') || '')

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

async function handleUserCommand(command) {
  if (command !== 'logout') return
  try {
    await ElMessageBox.confirm('确认退出登录？', '退出', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await apiLogout()
  } catch (e) {
    // 登出接口失败不阻塞本地退出
  }
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  localStorage.removeItem('currentRole')
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 30;
  height: var(--his-header-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  padding-right: 18px;
  border-right: 1px solid rgba(255, 255, 255, 0.22);
}

.brand-logo-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
  white-space: nowrap;
}

.breadcrumb-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
}

.crumb-icon {
  color: rgba(255, 255, 255, 0.7);
}

.crumb-sep {
  color: rgba(255, 255, 255, 0.5);
}

.crumb-current {
  color: #fff;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.header-user-role {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 2px 10px;
  line-height: 18px;
}

.header-user-name {
  font-size: 13px;
  color: #fff;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.3);
  margin: 0 4px;
}

.header-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  background: linear-gradient(135deg, #2B86CF, #1269B5);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s ease;
}

.header-avatar:hover {
  transform: scale(1.06);
}

.app-body {
  display: flex;
  margin-top: var(--his-header-h);
}

.app-sidebar {
  position: fixed;
  left: 0;
  top: var(--his-header-h);
  bottom: 0;
  z-index: 20;
  width: 208px;
  display: flex;
  flex-direction: column;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 10px 10px;
}

.sidebar-group {
  margin-bottom: 14px;
}

.sidebar-group-title {
  padding: 4px 12px 6px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: rgba(148, 163, 184, 0.75);
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  margin-bottom: 2px;
  border-radius: 8px;
  font-size: 13.5px;
  color: rgba(226, 232, 240, 0.85);
  text-decoration: none;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.sidebar-item-active {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-weight: 600;
}

.sidebar-item-icon {
  font-size: 17px;
  flex-shrink: 0;
}

.sidebar-footer {
  flex-shrink: 0;
  border-top: 1px solid rgba(255, 255, 255, 0.16);
  padding: 10px 14px;
}

.sidebar-footer-text {
  font-size: 11px;
  color: rgba(203, 213, 225, 0.55);
}

.app-main {
  flex: 1;
  min-width: 0;
  margin-left: 208px;
  height: calc(100vh - var(--his-header-h));
  overflow: auto;
  background: var(--his-bg);
}

.app-content {
  padding: var(--his-page-pad);
}
</style>
