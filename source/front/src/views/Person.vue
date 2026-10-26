<template>
  <div class="page-wrap">
    <h2 class="page-title">参保人信息</h2>
    <el-card>
      <template #header>参保人核验查询</template>
      <el-form inline size="small">
        <el-form-item label="身份证号"><el-input v-model="form.certNo" placeholder="18位身份证号" clearable style="width:220px"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.name" placeholder="参保人姓名" clearable style="width:160px"/></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="doQuery">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
      <div style="font-size:12px;color:#999;margin-bottom:8px">
        请求经前置机转发医保局参保查询接口（/insure/person/query）。开发期透传 csmis，生产期国密加签加密。
      </div>
      <el-alert v-if="errorMsg" :title="errorMsg" type="error" show-icon :closable="false" style="margin-bottom:12px"/>
      <el-descriptions v-if="summary" :column="2" border size="small">
        <el-descriptions-item label="返回码">{{ summary.infcode }}</el-descriptions-item>
        <el-descriptions-item label="返回信息">{{ summary.errMsg || summary.message || '-' }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ summary.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参保状态">{{ summary.insuStatus || summary.balance || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-collapse v-if="rawResponse" style="margin-top:12px">
        <el-collapse-item title="查看完整响应报文">
          <pre style="white-space:pre-wrap;word-break:break-all;font-size:12px;background:#f5f5f5;padding:10px;max-height:360px;overflow:auto">{{ rawResponse }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>
<script setup>
import { reactive, ref } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const form = reactive({ certNo: '', name: '' })
const loading = ref(false)
const errorMsg = ref('')
const summary = ref(null)
const rawResponse = ref('')

function reset() {
  form.certNo = ''; form.name = ''
  errorMsg.value = ''; summary.value = null; rawResponse.value = ''
}

async function doQuery() {
  if (!form.certNo) { ElMessage.warning('请输入身份证号'); return }
  loading.value = true
  errorMsg.value = ''; summary.value = null; rawResponse.value = ''
  try {
    // 参保查询走机器对机器路径（/insure/** 无需登录），开发期透传 csmis mock
    const body = { certType: '01', certNo: form.certNo.trim(), name: form.name.trim() || undefined }
    const res = await request.post('/insure/person/query', body)
    rawResponse.value = JSON.stringify(res.data, null, 2)
    const d = res.data && res.data.data
    summary.value = {
      infcode: d && d.infcode,
      errMsg: d && d.errMsg,
      message: res.data && res.data.message,
      name: d && (d.name || d.psnName),
      insuStatus: d && (d.insuStatus || d.insuInfo),
      balance: d && d.balance
    }
  } catch (e) {
    errorMsg.value = e.message || '查询失败'
  } finally {
    loading.value = false
  }
}
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
