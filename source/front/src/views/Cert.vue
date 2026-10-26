<template>
  <PlannedView
    title="密钥与证书"
    :points="points"
    basis="参考医保前置机方案「统一证书托管」：集中管理医保CA证书，统一完成平台身份认证；对应项目生产期 TODO（SM2签名、SM4加密、CA证书加载与轮换）。"
  />
</template>
<script setup>
import PlannedView from '../components/PlannedView.vue'
const points = [
  '证书列表：医院CA证书、医保局公钥证书的导入/查看/有效期提示',
  '证书轮换：到期前提醒、在线轮换、历史证书归档',
  '国密参数：SM2签名密钥、SM4报文加密密钥的托管与轮换',
  '加解密开关：开发期明文透传 ↔ 生产期国密加签加密一键切换'
]
</script>
