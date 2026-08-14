<template>
  <el-container style="min-height: 100vh">
    <el-aside width="220px" style="background: #304156">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid #1f2d3d">
        硬盘管理系统
      </div>
      <el-menu :default-active="route.path" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/disk">
          <el-icon><Folder /></el-icon>
          <span>硬盘管理</span>
        </el-menu-item>
        <el-menu-item index="/records">
          <el-icon><Document /></el-icon>
          <span>使用记录</span>
        </el-menu-item>
        <el-menu-item index="/rule/violations">
          <el-icon><Warning /></el-icon>
          <span>违规记录</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/rule/configs">
          <el-icon><Setting /></el-icon>
          <span>规则配置</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.isAdmin()" index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; border-bottom: 1px solid #e6e6e6">
        <span style="margin-right: 16px; color: #606266">
          {{ userStore.user.displayName || userStore.user.username }}
          <el-tag v-if="userStore.isAdmin()" type="danger" size="small" style="margin-left: 6px">管理员</el-tag>
          <el-tag v-else size="small" style="margin-left: 6px">用户</el-tag>
        </span>
        <el-button type="danger" plain size="small" @click="handleLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { DataAnalysis, Folder, Document, Warning, Setting, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>