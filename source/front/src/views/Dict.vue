<template>
  <PlannedView
    title="医保目录缓存"
    :points="points"
    basis="参考医保前置机方案「本地字典缓存」：离线存储药品、诊疗、参保人码表，减少专网交互频次；对照国家医保15项信息业务编码标准（药品、医用耗材、医疗服务项目、体外诊断试剂等）。"
  />
</template>
<script setup>
import PlannedView from '../components/PlannedView.vue'
const points = [
  '目录缓存概览：药品/诊疗项目/医用耗材/体外诊断试剂缓存量与更新时间',
  '目录下载与同步：调医保局目录下载接口（1xxx），增量更新本地缓存',
  '院内目录 ↔ 医保目录匹配：匹配关系查询、未匹配项提示',
  '缓存刷新策略：定时同步、手动刷新、版本比对'
]
</script>
