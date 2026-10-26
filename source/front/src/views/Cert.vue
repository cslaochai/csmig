<template>
  <div class="page-wrap">
    <h2 class="page-title">密钥与证书</h2>
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <el-statistic title="加签加密开关">
            <template #default>
              <el-tag :type="status.enabled ? 'success' : 'info'" size="large">{{ status.enabled ? '已启用' : '未启用' }}</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="运行模式">
            <template #default><span style="font-size:14px">{{ status.mode }}</span></template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="加密算法">
            <template #default><span style="font-size:14px">{{ status.algo }}</span></template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>证书与密钥清单</template>
      <el-table :data="certRows" size="small">
        <el-table-column prop="name" label="名称" width="200"/>
        <el-table-column prop="purpose" label="用途"/>
        <el-table-column label="当前状态" width="160">
          <template #default>
            <el-tag :type="status.enabled ? 'success' : 'info'" size="small">{{ status.enabled ? '启用中' : '待生产期配置' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="font-size:12px;color:#999;padding-top:10px">{{ status.note }}</div>
    </el-card>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const status = ref({ enabled: false, algo: '', mode: '', note: '' })

const certRows = [
  { name: '医院CA私钥证书', purpose: '对报文摘要做 SM2 签名（医院身份）' },
  { name: '医保局公钥证书', purpose: '验签医保局响应、加密对称密钥' },
  { name: 'SM4会话密钥', purpose: '医保报文正文加密' },
  { name: 'CA根证书链', purpose: '证书信任链校验与轮换' }
]

onMounted(async () => {
  const res = await request.get('/admin/crypto/status')
  Object.assign(status.value, res.data)
})
</script>
<style>
.page-title { margin:0 0 16px; font-size:18px; color:#134B77; font-weight:600; position:relative; padding-left:10px; }
.page-title::before { content:''; position:absolute; left:0; top:50%; transform:translateY(-50%); width:4px; height:16px; border-radius:2px; background:linear-gradient(180deg,#1269B5,#0E9488); }
</style>
