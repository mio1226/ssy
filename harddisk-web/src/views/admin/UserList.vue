<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h3>用户管理</h3>
      <el-button type="primary" @click="handleAdd">新增用户</el-button>
    </div>
    <el-card>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="搜索" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部" clearable style="width: 140px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="displayName" label="显示名称" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: center">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="prev, pager, next, total" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialog" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" :prop="isEdit ? '' : 'password'">
          <el-input v-model="form.password" type="password" :placeholder="isEdit ? '不填则不修改' : '请输入密码'" show-password />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="form.displayName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSave" :loading="saveLoading">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { listUsers, createUser, updateUser, deleteUser } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = reactive({ username: null, role: null })

const dialog = ref(false)
const saveLoading = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, username: '', password: '', displayName: '', email: '', phone: '', role: 'USER', status: 1 })
const rules = { username: [{ required: true, message: '请输入用户名' }] }

async function fetchData() {
  loading.value = true
  try {
    const res = await listUsers({ page: page.value, pageSize: pageSize.value, ...query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchData() }

function handleAdd() {
  isEdit.value = false
  form.id = null; form.username = ''; form.password = ''; form.displayName = ''
  form.email = ''; form.phone = ''; form.role = 'USER'; form.status = 1
  dialog.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  form.password = ''
  dialog.value = true
}

async function confirmSave() {
  const valid = await formRef.value.validate().catch(() => {})
  if (!valid) return
  saveLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser(form.id, form)
      ElMessage.success('更新成功')
    } else {
      if (!form.password) { ElMessage.warning('请输入密码'); return }
      await createUser(form)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    saveLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该用户？', '提示').then(async () => {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchData()
  }).catch(() => {})
}

onMounted(fetchData)
</script>
