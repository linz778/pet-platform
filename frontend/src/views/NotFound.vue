<template>
  <div class="notfound">
    <el-result icon="warning" title="404" sub-title="页面走丢了">
      <template #extra>
        <el-button type="primary" @click="$router.push('/portal')">回到首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.notfound {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
