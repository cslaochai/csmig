<template>
  <PlannedView
    title="异常告警"
    :points="points"
    basis="参考医保前置机方案的「实时监控与智能告警」：7×24 小时采集运行状态、自定义告警阈值、异常时短信/APP/平台消息三重预警。"
  />
</template>
<script setup>
import PlannedView from '../components/PlannedView.vue'
const points = [
  '失败交易实时提醒：交易失败/超时/结果未知时高亮并推送',
  '上游不可达告警：医保局平台连接失败、超时率超标预警',
  '告警规则配置：按报文类型、错误码、耗时阈值自定义规则',
  '告警记录与处置留痕：谁处理、何时处理、结果如何'
]
</script>
