<template>
  <div class="page-wrap">
    <h2 class="page-title">操作日志</h2>
    <el-card class="table-card">
      <el-form class="query-card" inline size="small" style="padding:14px 16px;margin-bottom:0">
        <el-form-item label="操作类型">
          <el-select v-model="query.action" clearable placeholder="全部" style="width:150px">
            <el-option v-for="(n, a) in ACTION_NAMES" :key="a" :label="n" :value="a"/>
          </el-select>
        </el-form-item>
        <el-form-item label="操作人"><el-input v-model="query.username" placeholder="登录账号" clearable style="width:160px"/></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="query.date" type="date" value-format="YYYY-MM-DD"/></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column label="操作" width="110">
          <template #default="{row}">
            <el-tag :type="actionType(row.action)" size="small">{{ actionName(row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="120"/>
        <el-table-column prop="target" label="操作对象" width="160" show-overflow-tooltip/>
        <el-table-column prop="detail" label="详情" show-overflow-tooltip/>
        <el-table-column prop="ip" label="IP" width="140"/>
        <el-table-column prop="createdAt" label="时间" width="160"/>
      </el-table>
      <div class="list-footer">
        <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" @current-change="load"/>
      </div>
      <div style="font-size:12px;color:#999">
        审计范围：登录/退出、手动与批量重发、目录缓存新增/删除、对账导出。留存管理后台关键操作，满足等保审计要求。
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, onMounted } from 'vue'
import request from '@/api/request'

const ACTION_NAMES = {
  LOGIN: '登录', LOGOUT: '退出', RESEND: '重发',
  DICT_CREATE: '目录新增', DICT_UPDATE: '目录修改', DICT_DELETE: '目录删除',
  DICT_DOWNLOAD: '目录下载',
  RECON_EXPORT: '对账导出', ALERT_RULE: '告警规则'
}
const actionName = a => ACTION_NAMES[a] || a
function actionType(a) {
  if (a === 'RESEND') return 'warning'
  if (a === 'DICT_DELETE') return 'danger'
  if (a === 'LOGIN' || a === 'LOGOUT') return 'info'
  return 'primary'
}

const query = reactive({ action: '', username: '', date: '' })
const list = reactive([])
let total = 0

async function load(page) {
  const params = { page, size: 20 }
  if (query.action) params.action = query.action
  if (query.username) params.username = query.username
  if (query.date) params.date = query.date
  const res = await request.get('/admin/audit/page', { params })
  total = res.data.total
  list.splice(0, list.length, ...res.data.list)
}
function reset() { query.action = ''; query.username = ''; query.date = ''; load(1) }

onMounted(() => load(1))
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
