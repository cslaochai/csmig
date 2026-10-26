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
          <div ref="byTypeRef" style="height:260px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>最近24小时失败趋势（按小时）</template>
          <div ref="hourlyRef" style="height:260px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>最近失败交易</template>
      <el-table :data="stats.recent" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column label="类型" width="170">
          <template #default="{row}"><span>{{ msgTypeName(row.msgType) }}</span></template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" width="180" show-overflow-tooltip/>
        <el-table-column prop="costMs" label="ms" width="80"/>
        <el-table-column prop="errMsg" label="失败原因" show-overflow-tooltip/>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>
        <span>告警规则</span>
        <el-button type="primary" size="small" plain style="margin-left:12px" @click="openCreate">新增规则</el-button>
      </template>
      <el-table :data="rules" size="small" empty-text="暂无规则">
        <el-table-column label="规则类型" width="150">
          <template #default="{row}"><span>{{ ruleTypeName(row.ruleType) }}</span></template>
        </el-table-column>
        <el-table-column label="阈值">
          <template #default="{row}">{{ ruleTypeUnit(row.ruleType) }} {{ row.threshold }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{row}">
            <el-switch :model-value="row.enabled === 1" @change="v => toggle(row, v)" size="small"/>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" show-overflow-tooltip/>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160"/>
      </el-table>
      <div style="font-size:12px;color:#999;padding-top:10px">
        规则由生产期定时任务按指标扫描触发（失败率/失败笔数/平均耗时超阈值），并推送至监控渠道；当前为规则配置入口。
      </div>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑告警规则' : '新增告警规则'" width="480px">
      <el-form :model="dialog.form" label-width="80px">
        <el-form-item label="规则类型" required>
          <el-select v-model="dialog.form.ruleType" style="width:100%">
            <el-option v-for="(n, t) in RULE_TYPES" :key="t" :label="n" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item :label="RULE_UNITS[dialog.form.ruleType] || '阈值'" required>
          <el-input-number v-model="dialog.form.threshold" :min="0" :precision="2" :step="1" style="width:160px"/>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="dialog.form.remark" placeholder="可选" maxlength="200"/></el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="dialog.form.enabled" :active-value="1" :inactive-value="0"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.loading" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { msgTypeName } from '@/lib/msgTypes'

const RULE_TYPES = { FAIL_RATE: '失败率', FAIL_COUNT: '失败笔数', AVG_COST: '平均耗时' }
const RULE_UNITS = { FAIL_RATE: '阈值（%）', FAIL_COUNT: '阈值（笔）', AVG_COST: '阈值（ms）' }
const ruleTypeName = t => RULE_TYPES[t] || t
const ruleTypeUnit = t => RULE_UNITS[t] || '阈值'

const stats = reactive({ todayTotal: 0, todayFail: 0, failRate: 0, byType: [], hourly: [], recent: [] })
const rules = ref([])
const byTypeRef = ref(null)
const hourlyRef = ref(null)
let byTypeChart = null
let hourlyChart = null

const dialog = reactive({
  visible: false, loading: false, id: null,
  form: { ruleType: 'FAIL_RATE', threshold: 10, enabled: 1, remark: '' }
})

function renderCharts() {
  if (byTypeRef.value && !byTypeChart) byTypeChart = echarts.init(byTypeRef.value)
  if (hourlyRef.value && !hourlyChart) hourlyChart = echarts.init(hourlyRef.value)
  if (byTypeChart) {
    byTypeChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
      xAxis: { type: 'category', data: stats.byType.map(t => msgTypeName(t.msgType)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', data: stats.byType.map(t => t.cnt), itemStyle: { color: '#E64340' }, barMaxWidth: 40 }]
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

async function loadStats() {
  const res = await request.get('/admin/alerts/summary')
  Object.assign(stats, res.data)
  renderCharts()
}
async function loadRules() {
  const res = await request.get('/admin/alerts/rules')
  rules.value = res.data || []
}

function openCreate() {
  dialog.id = null
  dialog.form = { ruleType: 'FAIL_RATE', threshold: 10, enabled: 1, remark: '' }
  dialog.visible = true
}
function openEdit(row) {
  dialog.id = row.id
  dialog.form = { ruleType: row.ruleType, threshold: Number(row.threshold), enabled: row.enabled, remark: row.remark }
  dialog.visible = true
}
async function saveRule() {
  dialog.loading = true
  try {
    if (dialog.id) {
      await request.put(`/admin/alerts/rules/${dialog.id}`, dialog.form)
    } else {
      await request.post('/admin/alerts/rules', dialog.form)
    }
    ElMessage.success('已保存')
    dialog.visible = false
    loadRules()
  } catch (e) { /* 拦截器已提示 */ } finally { dialog.loading = false }
}
async function toggle(row, v) {
  await request.put(`/admin/alerts/rules/${row.id}`, {
    ruleType: row.ruleType, threshold: Number(row.threshold), enabled: v ? 1 : 0, remark: row.remark
  })
  row.enabled = v ? 1 : 0
  ElMessage.success(v ? '已启用' : '已停用')
}
async function removeRule(row) {
  await ElMessageBox.confirm(`确认删除规则「${ruleTypeName(row.ruleType)}」？`, '删除', { type: 'warning' })
  await request.delete(`/admin/alerts/rules/${row.id}`)
  ElMessage.success('已删除')
  loadRules()
}

onMounted(() => { loadStats(); loadRules() })
onBeforeUnmount(() => {
  if (byTypeChart) { byTypeChart.dispose(); byTypeChart = null }
  if (hourlyChart) { hourlyChart.dispose(); hourlyChart = null }
})
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
