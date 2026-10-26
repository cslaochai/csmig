<template>
  <div>
    <h2 style="margin:0 0 16px;font-size:18px;color:#333">配置 & 对账</h2>
    <el-card style="margin-bottom:16px">
      <template #header>当前配置</template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="上游平台地址">{{ config.upstreamUrl }}</el-descriptions-item>
        <el-descriptions-item label="连接超时">{{ config.connectTimeoutMs }} ms</el-descriptions-item>
        <el-descriptions-item label="读取超时">{{ config.readTimeoutMs }} ms</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <el-card>
      <template #header>对账报表导出</template>
      <el-form inline>
        <el-form-item label="对账日期"><el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item><el-button type="primary" @click="exportCsv">导出CSV</el-button></el-form-item>
      </el-form>
      <div style="font-size:12px;color:#999">导出当日全部医保交易明细，可直接给财务/医保科对账。</div>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref, onMounted } from 'vue'
import axios from 'axios'
const config = reactive({ upstreamUrl:'', connectTimeoutMs:0, readTimeoutMs:0 })
const date = ref(new Date().toISOString().slice(0,10))
function exportCsv() { window.open('/mip/admin/recon/export?date=' + date.value) }
onMounted(async () => { const { data } = await axios.get('/mip/admin/config'); Object.assign(config, data) })
</script>
