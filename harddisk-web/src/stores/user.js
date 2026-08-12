import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || '{}'))

  function isLoggedIn() { return !!token.value }
  function isAdmin() { return user.value.role === 'ADMIN' }

  async function login(credentials) {
    const res = await loginApi(credentials)
    token.value = res.data.token
    user.value = { username: res.data.username, displayName: res.data.displayName, role: res.data.role }
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
    return res
  }

  async function register(data) {
    await registerApi(data)
  }

  function logout() {
    token.value = ''
    user.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, isAdmin, login, register, logout }
})
