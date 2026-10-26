<template>
  <div class="page-wrap">
    <h2 class="page-title">失败补偿</h2>
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
      <div style="font-size:12px;color:#999;padding:0 16px 10px">
        仅展示失败交易（success=0）。排查超时、结果未知等异常后可手动重发补偿，避免医保漏结算/重复扣费。
      </div>
      <el-table :data="list" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column prop="msgType" label="类型" width="100"/>
        <el-table-column prop="tradeNo" label="交易号" width="180" show-overflow-tooltip/>
        <el-table-column prop="costMs" label="ms" width="80"/>
        <el-table-column prop="errMsg" label="失败原因" show-overflow-tooltip/>
        <el-table-column label="操作" width="140">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="showBody(row)">报文</el-button>
            <el-button link type="warning" size="small" @click="resend(row)">重发</el-button>
          </template>
        </el-table-column>
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

const query = reactive({ msgType: '', tradeNo: '', date: '' })
const list = reactive([])
let total = 0
const body = reactive({ visible: false, title: '', content: '' })

async function load(page) {
  const res = await request.get('/admin/logs/page', { params: { page, size: 20, success: 0, ...query } })
  total = res.data.total
  list.splice(0, list.length, ...res.data.list)
}
function reset() { query.msgType = ''; query.tradeNo = ''; query.date = ''; load(1) }
function showBody(row) {
  body.title = '失败请求报文 #' + row.id
  body.content = row.requestBody || '(空)'
  body.visible = true
}
async function resend(row) {
  await ElMessageBox.confirm(`确认重发 ${row.tradeNo}？重发会再次调用医保局。`, '手动补偿', { type: 'warning' })
  await request.post(`/admin/logs/${row.id}/resend`)
  ElMessage.success('已重发')
  load(1)
}
onMounted(() => load(1))
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
