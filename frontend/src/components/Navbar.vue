<template>
  <header class="navbar">
    <button class="brand" type="button" @click="navigate('/')">
      <span class="brand-mark">校</span>
      <span>
        <strong>校园综合服务平台</strong>
        <small>Campus Service</small>
      </span>
    </button>

    <nav class="nav-links">
      <button
        v-for="item in navItems"
        :key="item.path"
        :class="{ active: route.path === item.path }"
        type="button"
        @click="navigate(item.path)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>

    <button class="logout-button" type="button" @click="logout">
      <el-icon><SwitchButton /></el-icon>
      <span>退出登录</span>
    </button>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Briefcase, ChatDotRound, Goods, House, List, Search, Setting, SwitchButton, User, Van } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const studentNavItems = [
  { label: '首页', path: '/', icon: House },
  { label: '拼车服务', path: '/carpool', icon: Van },
  { label: '兼职招聘', path: '/part-time', icon: Briefcase },
  { label: '闲置交易', path: '/secondhand', icon: Goods },
  { label: '失物招领', path: '/lost-found', icon: Search },
  { label: '消息交流', path: '/messages', icon: ChatDotRound },
  { label: '我的订单', path: '/orders', icon: List },
  { label: '个人中心', path: '/profile', icon: User }
]

const merchantNavItems = [
  { label: '首页', path: '/', icon: House },
  { label: '兼职招聘', path: '/part-time', icon: Briefcase },
  { label: '消息交流', path: '/messages', icon: ChatDotRound },
  { label: '个人中心', path: '/profile', icon: User }
]

const navItems = computed(() => {
  const items = userStore.user?.role === 'merchant' ? [...merchantNavItems] : [...studentNavItems]
  if (userStore.user?.role === 'admin') {
    items.push({ label: '后台管理', path: '/admin', icon: Setting })
  }
  return items
})

const navigate = (path) => {
  router.push(path)
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: minmax(220px, auto) minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  min-height: 64px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e6ebf2;
  box-shadow: 0 8px 24px rgba(31, 45, 61, 0.06);
  backdrop-filter: blur(10px);
}

button {
  font: inherit;
}

.brand,
.nav-links button,
.logout-button {
  display: inline-flex;
  align-items: center;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.brand {
  gap: 10px;
  color: #172033;
  text-align: left;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  color: #ffffff;
  font-weight: 800;
  background: #1f5eff;
  border-radius: 8px;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  line-height: 1.2;
}

.brand small {
  margin-top: 2px;
  color: #748294;
  font-size: 12px;
}

.nav-links {
  display: flex;
  justify-content: center;
  gap: 6px;
  min-width: 0;
}

.nav-links button {
  gap: 6px;
  height: 40px;
  padding: 0 12px;
  color: #526173;
  border-radius: 8px;
  transition: color 0.2s ease, background 0.2s ease;
}

.nav-links button:hover,
.nav-links button.active {
  color: #1f5eff;
  background: #edf4ff;
}

.logout-button {
  gap: 6px;
  height: 38px;
  padding: 0 12px;
  color: #d0472d;
  background: #fff1ed;
  border-radius: 8px;
}

@media (max-width: 960px) {
  .navbar {
    grid-template-columns: 1fr auto;
    padding: 10px 16px;
  }

  .nav-links {
    grid-column: 1 / -1;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }
}

@media (max-width: 520px) {
  .brand small,
  .logout-button span {
    display: none;
  }

  .nav-links button {
    padding: 0 10px;
    white-space: nowrap;
  }
}
</style>
