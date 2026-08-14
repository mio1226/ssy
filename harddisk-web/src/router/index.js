import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/Login.vue'), meta: { noAuth: true } },
  { path: '/register', name: 'Register', component: () => import('@/views/login/Register.vue'), meta: { noAuth: true } },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Dashboard.vue') },
      { path: 'disk', name: 'Disk', component: () => import('@/views/disk/DiskList.vue') },
      { path: 'disk/create', name: 'DiskCreate', component: () => import('@/views/disk/DiskForm.vue') },
      { path: 'disk/:id/edit', name: 'DiskEdit', component: () => import('@/views/disk/DiskForm.vue'), props: true, meta: { admin: true } },
      { path: 'disk/:id/records', name: 'DiskRecords', component: () => import('@/views/disk/DiskRecords.vue'), props: true },
      { path: 'records', name: 'RecordList', component: () => import('@/views/disk/RecordList.vue') },
      { path: 'rule/violations', name: 'Violations', component: () => import('@/views/rule/ViolationList.vue') },
      { path: 'rule/configs', name: 'RuleConfigs', component: () => import('@/views/rule/RuleConfig.vue'), meta: { admin: true } },
      { path: 'admin/users', name: 'AdminUsers', component: () => import('@/views/admin/UserList.vue'), meta: { admin: true } },
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  if (to.meta.noAuth) return next()
  const store = useUserStore()
  if (!store.isLoggedIn()) return next('/login')
  if (to.meta.admin && !store.isAdmin()) return next('/dashboard')
  next()
})

export default router