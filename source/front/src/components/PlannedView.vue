<template>
  <div>
    <h2 style="margin:0 0 16px;font-size:18px;color:#333">
      {{ title }}
      <el-tag size="small" type="info" style="margin-left:8px">规划中</el-tag>
    </h2>
    <el-card>
      <template #header>规划功能（参考国内HIS/医保前置机实现）</template>
      <ul style="margin:0;padding-left:20px;line-height:2">
        <li v-for="p in points" :key="p">{{ p }}</li>
      </ul>
    </el-card>
    <el-card style="margin-top:16px">
      <template #header>功能依据</template>
      <div style="font-size:13px;color:#666;line-height:1.8">{{ basis }}</div>
    </el-card>
  </div>
</template>
<script setup>
defineProps({
  title: { type: String, required: true },
  points: { type: Array, required: true },
  basis: { type: String, default: '' }
})
</script>
