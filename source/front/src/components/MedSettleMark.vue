<template>
  <svg
    class="med-settle-mark"
    :width="size"
    :height="size"
    viewBox="0 0 48 48"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    role="img"
    aria-label="医保结算前置系统"
  >
    <defs>
      <linearGradient :id="gradId" x1="8" y1="4" x2="40" y2="45" gradientUnits="userSpaceOnUse">
        <stop offset="0" stop-color="#1269B5" />
        <stop offset="1" stop-color="#0E9488" />
      </linearGradient>
    </defs>

    <!-- 盾形：保障与前置网关 -->
    <path
      d="M24 4 L40 10.3 V25.6 C40 34.7 33.7 41.2 24 44.5 C14.3 41.2 8 34.7 8 25.6 V10.3 Z"
      :fill="`url(#${gradId})`"
      stroke="#0B4F8A"
      stroke-opacity="0.18"
      stroke-width="1"
    />
    <!-- 医疗十字 -->
    <path
      d="M21.5 12.5 h5 v4.2 h4.2 v5 h-4.2 v4.2 h-5 v-4.2 h-4.2 v-5 h4.2 Z"
      fill="#FFFFFF"
    />
    <!-- 结算柱状：对账 / 汇总 -->
    <rect x="14.5" y="31" width="4.6" height="7" rx="1.3" fill="#FFFFFF" fill-opacity="0.82" />
    <rect x="21.7" y="27.5" width="4.6" height="10.5" rx="1.3" fill="#FFFFFF" fill-opacity="0.9" />
    <rect x="28.9" y="24" width="4.6" height="14" rx="1.3" fill="#FFFFFF" />
  </svg>
</template>

<script setup>
// 医保结算前置系统品牌徽标：盾形（保障/前置网关）+ 医疗十字 + 结算柱状（对账/汇总）。
// 纯内联 SVG，随尺寸矢量缩放，替代原先误用的 HIS 图标。
import { computed } from 'vue'

const props = defineProps({
  size: { type: [Number, String], default: 30 },
})

const gradId = computed(() => `med-settle-grad-${String(props.size).replace(/\D/g, '')}`)
</script>

<style scoped>
.med-settle-mark {
  display: block;
  flex: none;
}
</style>
