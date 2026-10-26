<template>
  <div class="page-wrap">
    <h2 class="page-title">基础配置</h2>
    <el-card>
      <template #header>当前配置</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="上游平台地址">{{ config.upstreamUrl }}</el-descriptions-item>
        <el-descriptions-item label="连接超时">{{ config.connectTimeoutMs }} ms</el-descriptions-item>
        <el-descriptions-item label="读取超时">{{ config.readTimeoutMs }} ms</el-descriptions-item>
      </el-descriptions>
      <div style="font-size:12px;color:#999;margin-top:12px">
        动态修改与保存功能规划中。生产期加签加密（SM2/SM4）、CA证书管理见「系统管理 → 密钥与证书」。
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import request from '@/api/request'
const config = reactive({ upstreamUrl: '', connectTimeoutMs: 0, readTimeoutMs: 0 })
onMounted(async () => {
  const res = await request.get('/admin/config')
  Object.assign(config, res.data)
})
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
