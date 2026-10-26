<template>
  <div class="page-wrap">
    <h2 class="page-title">异常告警</h2>
    <el-row :gutter="16">
      <el-col :span="8"><el-card><el-statistic title="今日总调用"><template #default>{{ stats.todayTotal }}</template></el-statistic></el-card></el-col>
      <el-col :span="8"><el-card><el-statistic title="今日失败"><template #default>{{ stats.todayFail }}</template></el-statistic></el-card></el-col>
      <el-col :span="8"><el-card><el-statistic title="失败率"><template #default>{{ stats.failRate }}%</template></el-statistic></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12">
        <el-card>
          <template #header>今日失败按报文类型分布</template>
          <div ref="byTypeRef" style="height:280px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近24小时失败趋势（按小时）</template>
          <div ref="hourlyRef" style="height:280px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>最近失败交易</template>
      <el-table :data="stats.recent" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="msgType" label="类型" width="100"/>
        <el-table-column prop="tradeNo" label="交易号" width="180" show-overflow-tooltip/>
        <el-table-column prop="costMs" label="ms" width="80"/>
        <el-table-column prop="errMsg" label="失败原因" show-overflow-tooltip/>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
      <div style="font-size:12px;color:#999;padding-top:10px">
        告警规则配置（阈值/推送渠道）与生产期对接，见「规划功能」；当前为失败交易实时面板。
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import request from '@/api/request'
import * as echarts from 'echarts'

const stats = reactive({ todayTotal: 0, todayFail: 0, failRate: 0, byType: [], hourly: [], recent: [] })
const byTypeRef = ref(null)
const hourlyRef = ref(null)
let byTypeChart = null
let hourlyChart = null

function renderCharts() {
  if (byTypeRef.value && !byTypeChart) byTypeChart = echarts.init(byTypeRef.value)
  if (hourlyRef.value && !hourlyChart) hourlyChart = echarts.init(hourlyRef.value)
  const types = stats.byType.map(t => t.msgType)
  const cnts = stats.byType.map(t => t.cnt)
  if (byTypeChart) {
    byTypeChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: types },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: cnts, itemStyle: { color: '#E64340' }, barMaxWidth: 40 }]
    })
  }
  if (hourlyChart) {
    hourlyChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: stats.hourly.map(h => h.hour + ':00') },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'line', data: stats.hourly.map(h => h.cnt), smooth: true,
                 itemStyle: { color: '#E64340' }, lineStyle: { color: '#E64340' }, areaStyle: { color: 'rgba(230,67,64,.12)' } }]
    })
  }
}

onMounted(async () => {
  const res = await request.get('/admin/alerts/summary')
  Object.assign(stats, res.data)
  renderCharts()
})

onBeforeUnmount(() => {
  if (byTypeChart) { byTypeChart.dispose(); byTypeChart = null }
  if (hourlyChart) { hourlyChart.dispose(); hourlyChart = null }
})
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
