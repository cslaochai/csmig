<template>
  <div class="page-wrap">
    <h2 class="page-title">交易流水</h2>
    <el-card class="table-card">
      <el-form class="query-card" inline size="small" style="padding:14px 16px;margin-bottom:0">
        <el-form-item label="报文类型"><el-input v-model="query.msgType" placeholder="2201/2301" clearable/></el-form-item>
        <el-form-item label="交易号"><el-input v-model="query.tradeNo" placeholder="HIS单号" clearable style="width:200px"/></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" :row-class-name="rowClass" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column label="类型" width="170">
          <template #default="{row}"><span>{{ msgTypeName(row.msgType) }}</span></template>
        </el-table-column>
        <el-table-column prop="tradeNo" label="交易号" width="180" show-overflow-tooltip/>
        <el-table-column label="结果" width="80">
          <template #default="{row}">
            <el-tag :type="row.success===1?'success':'danger'" size="small">{{ row.success===1?'成功':'失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="ms" width="80"/>
        <el-table-column prop="errMsg" label="错误" show-overflow-tooltip/>
        <el-table-column label="请求" width="70"><template #default="{row}"><el-button link size="small" @click="showBody(row,'requestBody')">查看</el-button></template></el-table-column>
        <el-table-column label="响应" width="70"><template #default="{row}"><el-button link size="small" @click="showBody(row,'responseBody')">查看</el-button></template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{row}"><el-button link type="warning" size="small" @click="resend(row)">重发</el-button></template></el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
      <div class="list-footer">
        <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" @current-change="load"/>
      </div>
    </el-card>
    <el-dialog v-model="body.visible" :title="body.title" width="700px">
      <pre style="white-space:pre-wrap;word-break:break-all;font-size:12px;max-height:400px;overflow:auto;background:#f5f5f5;padding:10px">{{ body.content }}</pre>
    </el-dialog>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessageBox, ElMessage } from 'element-plus'
import { msgTypeName } from '@/lib/msgTypes'
const query = reactive({ msgType:'', tradeNo:'', date:'' })
const list = reactive([]); let total = 0
const body = reactive({ visible:false, title:'', content:'' })
const rowClass = ({row}) => row.success===0 ? 'fail-row' : ''
async function load(page) {
  const res = await request.get('/admin/logs/page', { params: { page, size:20, ...query } })
  total = res.data.total; list.splice(0, list.length, ...res.data.list)
}
function reset() { query.msgType=''; query.tradeNo=''; query.date=''; load(1) }
function showBody(row, field) {
  body.title = (field==='requestBody'?'请求':'响应') + '报文 #' + row.id
  body.content = row[field] || '(空)'; body.visible = true
}
async function resend(row) {
  await ElMessageBox.confirm(`确认重发 ${row.tradeNo}？`, '手动补偿', { type:'warning' })
  await request.post(`/admin/logs/${row.id}/resend`)
  ElMessage.success('已重发'); load(1)
}
onMounted(() => load(1))
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
:deep(.fail-row) td { background:#fef0f0 !important; }
</style>
