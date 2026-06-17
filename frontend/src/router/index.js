import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/carpool',
    name: 'Carpool',
    component: () => import('../views/Carpool.vue'),
    meta: { disallowMerchant: true }
  },
  {
    path: '/part-time',
    name: 'PartTime',
    component: () => import('../views/PartTime.vue')
  },
  {
    path: '/secondhand',
    name: 'Secondhand',
    component: () => import('../views/Secondhand.vue'),
    meta: { disallowMerchant: true }
  },
  {
    path: '/lost-found',
    name: 'LostFound',
    component: () => import('../views/LostFound.vue'),
    meta: { disallowMerchant: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/Messages.vue')
  },
  {
    path: '/orders',
    name: 'Orders',
    component: () => import('../views/Orders.vue'),
    meta: { disallowMerchant: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue')
  },
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('../views/Forbidden.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const isLoggedIn = localStorage.getItem('token')
  let user = {}
  try {
    user = JSON.parse(localStorage.getItem('user') || '{}')
  } catch (error) {
    localStorage.removeItem('user')
  }
  if (to.path !== '/login' && to.path !== '/register' && !isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin) {
    next(user.role === 'admin' ? undefined : '/403')
  } else if (to.meta.disallowMerchant && user.role === 'merchant') {
    next('/profile')
  } else {
    next()
  }
})

export default router
