<template>
  <el-container style="min-height:100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="height:56px;line-height:56px;text-align:center;color:#fff;font-size:15px;font-weight:600;background:#1269B5">csmig 前置系统</div>
      <el-menu :default-active="$route.path" router style="background:#001529;border-right:0">
        <el-menu-item v-for="r in routes" :key="r.path" :index="r.path">
          <span>{{ r.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main style="background:#f0f2f5;padding:16px">
      <router-view />
    </el-main>
  </el-container>
</template>
<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
const routes = [
  { path: '/monitor', meta: { title: '接口监控' } },
  { path: '/logs', meta: { title: '交易流水' } },
  { path: '/config', meta: { title: '配置 & 对账' } }
]
</script>
<style>
.el-menu { --el-menu-bg-color:#001529; --el-menu-text-color:rgba(255,255,255,.75); --el-menu-active-color:#fff; --el-menu-hover-bg-color:transparent; }
.el-menu-item.is-active { background:#1269B5 !important; }
</style>
