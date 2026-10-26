<template>
  <PlannedView
    title="结算汇总"
    :points="points"
    basis="参考国家医保定点医药机构接口规范的日对账（dayBill）与 HIS 厂商的日结/月结报表：按结算类型汇总当日交易，与医保局日对账单比对。"
  />
</template>
<script setup>
import PlannedView from '../components/PlannedView.vue'
const points = [
  '日结算汇总：按挂号/门诊结算/退费/住院结算等类型统计金额与笔数',
  '日对账单比对：医保局 dayBill 报文 ↔ 本机交易流水自动核对',
  '月度结算统计：月结报表、趋势图表',
  '差异明细：医保侧有/本机无、本机有/医保侧无的异常单清单'
]
</script>
