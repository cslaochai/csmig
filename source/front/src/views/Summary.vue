<template>
  <div class="page-wrap">
    <h2 class="page-title">结算汇总</h2>
    <el-card>
      <template #header>
        <span>近{{ days }}日交易趋势（按报文类型）</span>
        <el-radio-group v-model="days" size="small" style="margin-left:16px" @change="load">
          <el-radio-button :value="7">7日</el-radio-button>
          <el-radio-button :value="14">14日</el-radio-button>
          <el-radio-button :value="30">30日</el-radio-button>
        </el-radio-group>
      </template>
      <div ref="chartRef" style="height:340px"></div>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>今日按报文类型汇总</template>
      <el-table :data="todayRows" size="small">
        <el-table-column prop="msgType" label="报文类型" width="140"/>
        <el-table-column prop="total" label="笔数"/>
        <el-table-column prop="success" label="成功"/>
        <el-table-column prop="fail" label="失败"/>
        <el-table-column label="成功率">
          <template #default="{row}">
            <el-tag :type="row.rate >= 99 ? 'success' : row.rate >= 95 ? 'warning' : 'danger'" size="small">{{ row.rate }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="font-size:12px;color:#999;padding-top:10px">
        金额口径：报文体费用字段解析与医保局 dayBill 日对账比对，见「结算汇总 → 日对账比对」规划。
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import request from '@/api/request'
import * as echarts from 'echarts'

const days = ref(7)
const rows = ref([])
const chartRef = ref(null)
let chart = null

// 今日（按 rows 中 day 等于当天；数据为空时兜底当天）
const today = new Date().toISOString().slice(0, 10)
const todayRows = computed(() => {
  const map = new Map()
  rows.value.filter(r => r.day === today).forEach(r => {
    const g = map.get(r.msgType) || { msgType: r.msgType, total: 0, success: 0, fail: 0 }
    g.total += Number(r.total); g.success += Number(r.success); g.fail += Number(r.fail)
    map.set(r.msgType, g)
  })
  return [...map.values()].map(g => ({
    ...g, rate: g.total === 0 ? 100 : Math.round(g.success * 1000.0 / g.total) / 10
  }))
})

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const dates = [...new Set(rows.value.map(r => r.day))].sort()
  const types = [...new Set(dates.flatMap(d => rows.value.filter(r => r.day === d).map(r => r.msgType)))]
  const totalMap = new Map(rows.value.map(r => [r.day + '|' + r.msgType, Number(r.total)]))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: types },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', minInterval: 1 },
    series: types.map(t => ({
      name: t, type: 'bar', stack: 'total',
      data: dates.map(d => totalMap.get(d + '|' + t) || 0)
    }))
  })
}

async function load() {
  const res = await request.get('/admin/summary/daily', { params: { days: days.value } })
  rows.value = res.data.rows || []
  renderChart()
}

onMounted(load)
onBeforeUnmount(() => { if (chart) { chart.dispose(); chart = null } })
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
