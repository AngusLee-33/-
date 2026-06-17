import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi, userApi } from '../api'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('token') || '')

  const login = async (username, password) => {
    const response = await authApi.login({ username, password })
    if (response.code === 200) {
      token.value = response.data.token
      user.value = response.data.user
      localStorage.setItem('token', response.data.token)
      localStorage.setItem('user', JSON.stringify(response.data.user))
    }
    return response
  }

  const register = async (data) => {
    const response = await authApi.register(data)
    return response
  }

  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  const getProfile = async () => {
    const response = await userApi.getProfile()
    if (response.code === 200) {
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    }
    return response
  }

  const initUser = () => {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        user.value = JSON.parse(storedUser)
      } catch (error) {
        user.value = null
        token.value = ''
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }
    }
  }

  return {
    user,
    token,
    login,
    register,
    logout,
    getProfile,
    initUser
  }
})
