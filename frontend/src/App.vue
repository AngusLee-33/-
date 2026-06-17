<template>
  <div class="app">
    <Navbar v-if="isLoggedIn" />
    <router-view />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from './stores/user'
import Navbar from './components/Navbar.vue'

const userStore = useUserStore()
const route = useRoute()
const authPages = ['/login', '/register']
const isLoggedIn = computed(() => Boolean(userStore.token) && !authPages.includes(route.path))

onMounted(() => {
  userStore.initUser()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", Arial, sans-serif;
  background-color: #f5f5f5;
}

.app {
  min-height: 100vh;
}
</style>
