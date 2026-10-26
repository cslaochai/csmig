<template>
  <div class="page-wrap">
    <h2 class="page-title">对账报表</h2>
    <el-row :gutter="16">
      <el-col :span="5"><el-card><el-statistic title="总笔数"><template #default>{{ summary.total }}</template></el-statistic></el-card></el-col>
      <el-col :span="5"><el-card><el-statistic title="成功"><template #default>{{ summary.success }}</template></el-statistic></el-card></el-col>
      <el-col :span="5"><el-card><el-statistic title="失败"><template #default>{{ summary.fail }}</template></el-statistic></el-card></el-col>
      <el-col :span="5"><el-card><el-statistic title="成功率"><template #default>{{ summary.successRate }}%</template></el-statistic></el-card></el-col>
      <el-col :span="4"><el-card><el-statistic title="平均耗时"><template #default>{{ summary.avgCostMs }}ms</template></el-statistic></el-card></el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>交易对账导出</template>
      <el-form inline size="small">
        <el-form-item label="对账日期"><el-date-picker v-model="date" type="date" value-format="YYYY-MM-DD" @change="loadSummary"/></el-form-item>
        <el-form-item label="报文类型">
          <el-select v-model="exportMsgType" clearable placeholder="全部" style="width:170px">
            <el-option v-for="(n, t) in MSG_TYPES" :key="t" :label="n + ' (' + t + ')'" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="failOnly" style="width:130px">
            <el-option label="全部交易" :value="false"/>
            <el-option label="仅失败" :value="true"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="exporting" @click="exportCsv">导出CSV</el-button>
        </el-form-item>
      </el-form>
      <div style="font-size:12px;color:#999">
        按当日流水导出（可筛选类型/仅失败），供财务与医保科对账。金额口径与医保局 dayBill 日对账单比对见「结算汇总」。
      </div>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>今日按报文类型分布</template>
      <el-table :data="typeRows" size="small">
        <el-table-column label="报文类型" width="200">
          <template #default="{row}"><span>{{ msgTypeName(row.msgType) }}</span></template>
        </el-table-column>
        <el-table-column prop="total" label="笔数"/>
        <el-table-column prop="success" label="成功"/>
        <el-table-column prop="fail" label="失败"/>
        <el-table-column label="成功率">
          <template #default="{row}">
            <el-tag :type="row.rate >= 99 ? 'success' : row.rate >= 95 ? 'warning' : 'danger'" size="small">{{ row.rate }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { msgTypeName, MSG_TYPES } from '@/lib/msgTypes'

const date = ref(new Date().toISOString().slice(0, 10))
const exportMsgType = ref('')
const failOnly = ref(false)
const exporting = ref(false)
const summary = reactive({ total: 0, success: 0, fail: 0, successRate: 100, avgCostMs: 0, byType: {} })

const typeRows = computed(() => Object.entries(summary.byType).map(([msgType, arr]) => ({
  msgType, total: arr[0], success: arr[1], fail: arr[0] - arr[1],
  rate: arr[0] === 0 ? 100 : Math.round(arr[1] * 1000.0 / arr[0]) / 10
})))

async function loadSummary() {
  const res = await request.get('/admin/recon/summary', { params: { date: date.value } })
  Object.assign(summary, res.data)
}

async function exportCsv() {
  if (!date.value) {
    ElMessage.warning('请选择对账日期')
    return
  }
  exporting.value = true
  try {
    // 带登录态下载：blob 方式，拦截器对 blob 直接放行
    const params = { date: date.value }
    if (exportMsgType.value) params.msgType = exportMsgType.value
    if (failOnly.value) params.failOnly = true
    const blob = await request.get('/admin/recon/export', { params, responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `recon-${date.value}${exportMsgType.value ? '-' + exportMsgType.value : ''}${failOnly.value ? '-fail' : ''}.csv`
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

onMounted(loadSummary)
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
