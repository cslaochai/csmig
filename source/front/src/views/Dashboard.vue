<template>
  <div class="page-wrap">
    <h2 class="page-title">实时交易看板</h2>
    <el-row :gutter="16">
      <el-col :span="6"><el-card><el-statistic title="今日总调用"><template #default>{{ stats.total }}</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="成功率"><template #default>{{ stats.successRate }}%</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="失败"><template #default>{{ stats.fail }}</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="平均耗时"><template #default>{{ stats.avgCostMs }}ms</template></el-statistic></el-card></el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>今日按报文类型分布（2201挂号 / 2301门诊结算 / 2304退费 / dayBill日对账等）</template>
      <div ref="chartRef" style="height:360px"></div>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>最新失败交易</template>
      <el-table :data="stats.recent" size="small" empty-text="今日无失败交易">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="msgType" label="类型" width="120"/>
        <el-table-column prop="tradeNo" label="交易号" width="200" show-overflow-tooltip/>
        <el-table-column label="结果" width="80">
          <template #default="{row}">
            <el-tag :type="row.success===1?'success':'danger'" size="small">{{ row.success===1?'成功':'失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errMsg" label="错误" show-overflow-tooltip/>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import request from '@/api/request'
import * as echarts from 'echarts'

const stats = reactive({ total: 0, success: 0, fail: 0, successRate: 100, avgCostMs: 0, byType: {}, recent: [] })
const chartRef = ref(null)
let chart = null

function renderChart(byType) {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const types = Object.keys(byType).sort()
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['总调用', '成功'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: types, name: '报文类型' },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '总调用', type: 'bar', data: types.map(t => byType[t][0]), itemStyle: { color: '#1269B5' } },
      { name: '成功', type: 'bar', data: types.map(t => byType[t][1]), itemStyle: { color: '#67C23A' } }
    ]
  })
}

onMounted(async () => {
  const res = await request.get('/admin/stats/today')
  Object.assign(stats, res.data)
  renderChart(res.data.byType || {})
})

onBeforeUnmount(() => { if (chart) { chart.dispose(); chart = null } })
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
