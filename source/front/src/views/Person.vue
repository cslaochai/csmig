<template>
  <PlannedView
    title="参保人信息"
    :points="points"
    basis="参考国家医保接口规范中的参保人/人员信息查询类交易（如结算信息查询5203）与前置机的参保人码表缓存：支撑挂号、结算前的身份核验。"
  />
</template>
<script setup>
import PlannedView from '../components/PlannedView.vue'
const points = [
  '参保人身份核验：通过医保电子凭证/身份证号调用医保局查询接口',
  '参保状态查询：参保/停保/待遇状态展示',
  '结算信息查询：按条件查本机构一段时间的结算记录（对应5203）',
  '常用参保人缓存：本地缓存最近核验记录，降低专网交互频次'
]
</script>
