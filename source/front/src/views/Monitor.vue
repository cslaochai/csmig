<template>
  <div>
    <h2 style="margin:0 0 16px;font-size:18px;color:#333">接口监控</h2>
    <el-row :gutter="16">
      <el-col :span="6"><el-card><el-statistic title="今日总调用"><template #default>{{ stats.total }}</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="成功率"><template #default>{{ stats.successRate }}%</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="失败"><template #default>{{ stats.fail }}</template></el-statistic></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="平均耗时"><template #default>{{ stats.avgCostMs }}ms</template></el-statistic></el-card></el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>最近调用</template>
      <el-table :data="stats.recent" :row-class-name="rowClass" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="msgType" label="类型" width="120"/>
        <el-table-column prop="tradeNo" label="交易号" width="200" show-overflow-tooltip/>
        <el-table-column label="结果" width="80">
          <template #default="{row}">
            <el-tag :type="row.success===1?'success':'danger'" size="small">{{ row.success===1?'成功':'失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="90"/>
        <el-table-column prop="errMsg" label="错误" show-overflow-tooltip/>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import axios from 'axios'
const stats = reactive({ total:0, success:0, fail:0, successRate:100, avgCostMs:0, recent:[] })
const rowClass = ({row}) => row.success===0 ? 'fail-row' : ''
onMounted(async () => {
  const { data } = await axios.get('/mip/admin/stats/today')
  Object.assign(stats, data)
})
</script>
<style>
:deep(.fail-row) td { background: #fef0f0 !important; }
</style>
