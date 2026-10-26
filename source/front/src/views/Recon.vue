<template>
  <div class="page-wrap">
    <h2 class="page-title">对账报表</h2>
    <el-card>
      <template #header>交易对账导出</template>
      <el-form inline>
        <el-form-item label="对账日期"><el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="exporting" @click="exportCsv">导出CSV</el-button>
        </el-form-item>
      </el-form>
      <div style="font-size:12px;color:#999">
        导出当日全部医保交易明细（含报文类型、交易号、成功状态、耗时、错误、时间），可直接给财务/医保科对账。
        后续可扩展：日对账单比对（医保局 dayBill 报文 ↔ 本机流水）、结算汇总。
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
const date = ref(new Date().toISOString().slice(0, 10))
const exporting = ref(false)

async function exportCsv() {
  if (!date.value) {
    ElMessage.warning('请选择对账日期')
    return
  }
  exporting.value = true
  try {
    // 带登录态下载：window.open 不带 Authorization 头，改用 blob 方式
    const res = await request.get('/admin/recon/export', {
      params: { date: date.value },
      responseType: 'blob'
    })
    const blob = new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `recon-${date.value}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败：' + (e.message || '请稍后重试'))
  } finally {
    exporting.value = false
  }
}
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
