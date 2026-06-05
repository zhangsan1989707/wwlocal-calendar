<template>
  <section class="page-panel">
    <div class="page-heading">
      <h1>管理概览</h1>
      <p>统一查看企业日历资源、日程流转和系统运行状态。</p>
    </div>
    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const metrics = [
  { label: '启用日历', value: '128' },
  { label: '本月日程', value: '642' },
  { label: '共享成员', value: '1,268' },
  { label: '待处理事项', value: '9' }
];
</script>
