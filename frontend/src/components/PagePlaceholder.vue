<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ title }}</span>
          <el-tag type="info" effect="plain">脚手架占位页</el-tag>
        </div>
      </template>
      <el-empty :description="desc || '该模块待开发，可在此接入对应后端接口。'" />
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' }
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
