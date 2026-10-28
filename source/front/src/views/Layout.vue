<template>
  <div class="app-shell">
    <!-- 顶部 Header：与 cslk HIS 同风格（深蓝顶栏 + logo + 面包屑 + 用户区） -->
    <header class="his-header app-header">
      <div class="header-left">
        <router-link to="/" class="brand" title="返回首页" :style="brandStyle">
          <div class="brand-logo-wrap">
            <MedSettleMark :size="30" />
          </div>
          <span v-if="!sidebarCollapsed" class="brand-name">医保结算前置系统</span>
        </router-link>
        <button
          class="collapse-btn"
          :title="sidebarCollapsed ? '展开菜单' : '收起菜单'"
          @click="toggleSidebar"
        >
          <el-icon v-if="sidebarCollapsed"><Expand /></el-icon>
          <el-icon v-else><Fold /></el-icon>
        </button>
        <nav class="breadcrumb-nav">
          <el-icon class="crumb-icon"><HomeFilled /></el-icon>
          <span class="crumb-label">医保结算前置系统</span>
          <el-icon v-if="currentTitle" class="crumb-sep"><ArrowRight /></el-icon>
          <span v-if="currentTitle" class="crumb-current">{{ currentTitle }}</span>
        </nav>
      </div>
      <div class="header-right">
        <el-badge :value="0" :hidden="true">
          <button class="header-icon-btn" title="消息通知">
            <el-icon><Bell /></el-icon>
          </button>
        </el-badge>
        <div class="header-divider"></div>
        <div class="header-user">
          <span class="header-user-name">{{ realName || username }}</span>
          <span class="header-user-role">{{ roleLabel }}</span>
        </div>
        <div class="header-divider"></div>
        <div class="header-clock">
          <el-icon><Calendar /></el-icon>
          <span class="clock-time">{{ currentTime }}</span>
          <span class="clock-week">{{ currentWeekday }}</span>
        </div>
        <div class="header-divider"></div>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <button class="header-icon-btn" title="设置">
            <el-icon><Setting /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item divided command="logout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 下方：左侧导航 + 主内容 -->
    <div class="app-body" :class="{ 'sidebar-collapsed': sidebarCollapsed }">
      <aside class="his-sidebar app-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <el-menu
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          :default-active="activeMenu"
          router
          class="app-menu"
        >
          <el-sub-menu v-for="group in menuGroups" :key="group.title" :index="group.title">
            <template #title>
              <el-icon><component :is="group.icon" /></el-icon>
              <span>{{ group.title }}</span>
            </template>
            <el-menu-item
              v-for="item in group.children"
              :key="item.path"
              :index="item.path"
              :title="item.title"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
        <div class="sidebar-footer">
          <span class="sidebar-footer-text">医保结算前置系统 · v1.0.0</span>
        </div>
      </aside>

      <div class="app-main">
        <main class="app-content">
          <router-view />
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Bell,
  Calendar,
  DataLine,
  Expand,
  Files,
  Fold,
  HomeFilled,
  Key,
  List,
  Memo,
  Odometer,
  RefreshLeft,
  Setting,
  SwitchButton,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import { logout as apiLogout } from '@/api/system'
import MedSettleMark from '@/components/MedSettleMark.vue'

const route = useRoute()
const router = useRouter()

// 一级菜单按业务域分组；结构与国内 HIS/医保前置机管理后台一致（监控/交易/对账/字典/系统）
const menuGroups = [
  {
    title: '运行监控',
    icon: Odometer,
    children: [
      { path: '/monitor', title: '接口监控', icon: Odometer },
      { path: '/dashboard', title: '实时交易看板', icon: Tickets },
      { path: '/alerts', title: '异常告警', icon: Bell },
    ],
  },
  {
    title: '交易管理',
    icon: List,
    children: [
      { path: '/logs', title: '交易流水', icon: List },
      { path: '/compensation', title: '失败补偿', icon: RefreshLeft },
    ],
  },
  {
    title: '对账结算',
    icon: Tickets,
    children: [
      { path: '/recon', title: '对账报表', icon: Tickets },
      { path: '/summary', title: '结算汇总', icon: DataLine },
    ],
  },
  {
    title: '字典目录',
    icon: Files,
    children: [
      { path: '/dict', title: '医保目录缓存', icon: Files },
      { path: '/person', title: '参保人信息', icon: User },
    ],
  },
  {
    title: '系统管理',
    icon: Setting,
    children: [
      { path: '/config', title: '基础配置', icon: Setting },
      { path: '/cert', title: '密钥与证书', icon: Key },
      { path: '/audit', title: '操作日志', icon: Memo },
    ],
  },
]

const currentTitle = computed(() => route.meta?.title || '')

const roleLabel = computed(() => {
  const role = localStorage.getItem('currentRole') || 'ADMIN'
  return role === 'ADMIN' ? '系统管理员' : role
})

const realName = computed(() => localStorage.getItem('realName') || '')
const username = computed(() => localStorage.getItem('username') || '')

const activeMenu = computed(() => route.path)

function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

// ========== 侧栏折叠（与 HIS 一致：按钮在顶栏，侧栏收成图标） ==========
const sidebarCollapsed = ref(false)
function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

// 顶栏品牌区宽度与左侧菜单栏对齐（抄 HIS）：
// 展开 240px + 顶栏 24px 左内边距 = 侧栏 264px，右边缘对齐并带竖线；
// 折叠时 marginLeft -24px + 宽 64px，右边缘对齐侧栏折叠态 64px。
const brandStyle = computed(() =>
  sidebarCollapsed.value
    ? { width: '64px', marginLeft: '-24px', justifyContent: 'center' }
    : { width: '240px' }
)

// ========== 实时时钟 ==========
const currentTime = ref('')
const currentWeekday = ref('')
const dayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
let clockTimer = null

const updateClock = () => {
  const now = new Date()
  const y = now.getFullYear()
  const mon = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${mon}-${d} ${hh}:${mm}:${ss}`
  currentWeekday.value = dayNames[now.getDay()]
}

onMounted(() => {
  updateClock()
  clockTimer = setInterval(updateClock, 1000)
})

onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
})

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
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
  overflow: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.3);
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

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 1px;
  white-space: nowrap;
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
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

.header-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  font-size: 18px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.header-icon-btn:hover {
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.header-divider {
  width: 1px;
  height: 20px;
  background: rgba(255, 255, 255, 0.3);
  margin: 0 4px;
}

.header-user {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
}

.header-user-name {
  font-size: 13px;
  color: #fff;
  font-weight: 500;
}

.header-user-role {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
}

.header-clock {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #fff;
  font-size: 13px;
}

.clock-time {
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.clock-week {
  color: rgba(255, 255, 255, 0.85);
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
  width: 264px;
  background: #0a3d6b;
  display: flex;
  flex-direction: column;
  transition: width 0.2s ease;
}

.app-sidebar.is-collapsed {
  width: 64px;
}

.app-menu {
  flex: 1;
  overflow-y: auto;
  border-right: none;
  background: transparent;
}

.app-menu :deep(.el-menu-item),
.app-menu :deep(.el-sub-menu__title) {
  color: rgba(226, 232, 240, 0.85);
  background: transparent;
}

.app-menu :deep(.el-sub-menu__title:hover),
.app-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.app-menu :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-weight: 600;
}

.app-menu :deep(.el-menu--collapse) {
  width: 64px;
}

/* 子菜单展开面板透出侧栏底色（#0A3D6B），避免 Element Plus 默认白底在深蓝侧栏里露白 */
.app-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}

.app-sidebar.is-collapsed .sidebar-footer {
  display: none;
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
  margin-left: 264px;
  height: calc(100vh - var(--his-header-h));
  overflow: auto;
  background: var(--his-bg);
  transition: margin-left 0.2s ease;
}

.app-body.sidebar-collapsed .app-main {
  margin-left: 64px;
}

.app-content {
  padding: var(--his-page-pad);
}
</style>
