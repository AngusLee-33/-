<template>
  <main class="forbidden-page">
    <section class="forbidden-panel">
      <el-result
        icon="warning"
        title="无权限访问"
        sub-title="当前账号没有访问该页面的权限。"
      >
        <template #extra>
          <el-button type="primary" @click="router.push('/')">返回首页</el-button>
          <el-button @click="router.push('/profile')">个人中心</el-button>
        </template>
      </el-result>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.forbidden-page {
  min-height: calc(100vh - 64px);
  display: grid;
  place-items: center;
  padding: 24px;
  background: #f4f7fb;
}

.forbidden-panel {
  width: min(560px, 100%);
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}
</style>
