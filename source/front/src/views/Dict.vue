<template>
  <div class="page-wrap">
    <h2 class="page-title">医保目录缓存</h2>
    <el-row :gutter="16">
      <el-col v-for="s in statRowsComputed" :key="s.dictType" :span="6">
        <el-card>
          <el-statistic :title="typeName(s.dictType)">
            <template #default>{{ s.total }}</template>
          </el-statistic>
          <div style="font-size:12px;color:#999">已匹配 {{ s.matched }} · 待匹配 {{ s.total - s.matched }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="table-card" style="margin-top:16px">
      <el-form class="query-card" inline size="small" style="padding:14px 16px;margin-bottom:0">
        <el-form-item label="目录类型">
          <el-select v-model="query.dictType" clearable placeholder="全部" style="width:150px">
            <el-option v-for="(n, t) in TYPE_NAMES" :key="t" :label="n" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item label="关键词"><el-input v-model="query.keyword" placeholder="院内/医保编码或名称" clearable style="width:220px"/></el-form-item>
        <el-form-item label="匹配状态">
          <el-select v-model="query.matchStatus" clearable placeholder="全部" style="width:120px">
            <el-option label="已匹配" :value="1"/>
            <el-option label="未匹配" :value="0"/>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load(1)">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" plain @click="openCreate">新增</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" size="small">
        <el-table-column prop="id" label="ID" width="70"/>
        <el-table-column label="目录类型" width="110">
          <template #default="{row}"><el-tag size="small">{{ typeName(row.dictType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="hisCode" label="院内编码" width="140" show-overflow-tooltip/>
        <el-table-column prop="hisName" label="院内名称" show-overflow-tooltip/>
        <el-table-column prop="mediCode" label="医保编码" width="150" show-overflow-tooltip/>
        <el-table-column prop="mediName" label="医保名称" show-overflow-tooltip/>
        <el-table-column label="匹配状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.matchStatus === 1 ? 'success' : 'info'" size="small">{{ row.matchStatus === 1 ? '已匹配' : '未匹配' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160"/>
      </el-table>
      <div class="list-footer">
        <el-pagination layout="total, prev, pager, next" :total="total" :page-size="20" @current-change="load"/>
      </div>
    </el-card>

    <el-dialog v-model="create.visible" :title="create.id ? '编辑目录缓存' : '新增目录缓存'" width="520px">
      <el-form :model="create.form" label-width="90px">
        <el-form-item label="目录类型" required>
          <el-select v-model="create.form.dictType" style="width:100%">
            <el-option v-for="(n, t) in TYPE_NAMES" :key="t" :label="n" :value="t"/>
          </el-select>
        </el-form-item>
        <el-form-item label="院内编码" required><el-input v-model="create.form.hisCode"/></el-form-item>
        <el-form-item label="院内名称" required><el-input v-model="create.form.hisName"/></el-form-item>
        <el-form-item label="医保编码"><el-input v-model="create.form.mediCode"/></el-form-item>
        <el-form-item label="医保名称"><el-input v-model="create.form.mediName"/></el-form-item>
        <el-form-item label="匹配状态">
          <el-switch v-model="create.form.matchStatus" :active-value="1" :inactive-value="0" active-text="已匹配" inactive-text="未匹配"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="create.visible = false">取消</el-button>
        <el-button type="primary" :loading="create.loading" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const TYPE_NAMES = { DRUG: '药品', DIAG: '诊疗项目', MAT: '医用耗材', IVD: '体外诊断试剂' }
const typeName = t => TYPE_NAMES[t] || t

const query = reactive({ dictType: '', keyword: '', matchStatus: '' })
const list = reactive([])
let total = 0

const statRows = ref([])
const statRowsComputed = computed(() => {
  const types = Object.keys(TYPE_NAMES)
  return types.map(t => {
    const s = statRows.value.find(r => r.dictType === t)
    return { dictType: t, total: s ? Number(s.total) : 0, matched: s ? Number(s.matched) : 0 }
  })
})

const create = reactive({
  visible: false, loading: false, id: null,
  form: { dictType: 'DRUG', hisCode: '', hisName: '', mediCode: '', mediName: '', matchStatus: 0 }
})

async function load(page) {
  const params = { page, size: 20 }
  if (query.dictType) params.dictType = query.dictType
  if (query.keyword) params.keyword = query.keyword
  if (query.matchStatus !== '') params.matchStatus = query.matchStatus
  const res = await request.get('/admin/dict/page', { params })
  total = res.data.total
  list.splice(0, list.length, ...res.data.list)
}
function reset() { query.dictType = ''; query.keyword = ''; query.matchStatus = ''; load(1) }

async function loadStats() {
  const res = await request.get('/admin/dict/stats')
  statRows.value = res.data || []
}

function openCreate() {
  create.id = null
  create.form = { dictType: 'DRUG', hisCode: '', hisName: '', mediCode: '', mediName: '', matchStatus: 0 }
  create.visible = true
}
function openEdit(row) {
  create.id = row.id
  create.form = { dictType: row.dictType, hisCode: row.hisCode, hisName: row.hisName,
                  mediCode: row.mediCode || '', mediName: row.mediName || '', matchStatus: row.matchStatus }
  create.visible = true
}
async function submitCreate() {
  create.loading = true
  try {
    if (create.id) {
      await request.put(`/admin/dict/${create.id}`, create.form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/admin/dict', create.form)
      ElMessage.success('新增成功')
    }
    create.visible = false
    load(1); loadStats()
  } catch (e) { /* 错误已由拦截器提示 */ } finally { create.loading = false }
}
async function remove(row) {
  await ElMessageBox.confirm(`确认删除 ${row.hisName}（${row.hisCode}）？`, '删除', { type: 'warning' })
  await request.delete(`/admin/dict/${row.id}`)
  ElMessage.success('已删除')
  load(1); loadStats()
}

onMounted(() => { load(1); loadStats() })
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
