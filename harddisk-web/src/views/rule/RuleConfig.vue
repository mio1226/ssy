<template>
  <div>
    <h3 style="margin-bottom: 16px">规则配置</h3>
    <el-alert v-if="feishuNotConfigured" title="飞书导出未配置" type="warning" :closable="false" show-icon style="margin-bottom: 16px">
      飞书 App ID、App Secret 或表格 Token 未填写，导出功能不可用，请在此页面配置。
    </el-alert>
    <el-card>
      <div style="margin-bottom: 16px">
        <el-button type="primary" @click="handleAdd">新增规则</el-button>
      </div>
      <el-table :data="configs" border stripe v-loading="loading">
        <el-table-column prop="ruleKey" label="规则键" width="200" />
        <el-table-column prop="ruleValue" label="规则值" width="200" />
        <el-table-column prop="description" label="描述" min-width="300" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editDialog" :title="isAdd ? '新增规则' : '编辑规则'" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="规则键">
          <el-input v-if="isAdd" v-model="editForm.ruleKey" placeholder="请输入规则键" />
          <span v-else>{{ editForm.ruleKey }}</span>
        </el-form-item>
        <el-form-item label="规则值">
          <el-input v-model="editForm.ruleValue" :type="editForm.ruleKey === 'feishu_app_secret' ? 'password' : 'text'" show-password />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="editForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="editLoading">{{ isAdd ? '新增' : '保存' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRuleConfigs, createRuleConfig, updateRuleConfig, deleteRuleConfig } from '@/api/rule'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const configs = ref([])
const feishuNotConfigured = ref(false)
const editDialog = ref(false)
const editLoading = ref(false)
const isAdd = ref(false)
const editForm = ref({ id: null, ruleKey: '', ruleValue: '', description: '', status: 1 })

async function fetchData() {
  loading.value = true
  try {
    const res = await getRuleConfigs()
    configs.value = res.data
    const appId = res.data.find(c => c.ruleKey === 'feishu_app_id')
    const secret = res.data.find(c => c.ruleKey === 'feishu_app_secret')
    const token = res.data.find(c => c.ruleKey === 'feishu_spreadsheet_token')
    feishuNotConfigured.value = !appId?.ruleValue || !secret?.ruleValue || !token?.ruleValue
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isAdd.value = true
  editForm.value = { id: null, ruleKey: '', ruleValue: '', description: '', status: 1 }
  editDialog.value = true
}

function handleEdit(row) {
  isAdd.value = false
  editForm.value = { ...row }
  editDialog.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除规则「' + row.ruleKey + '」吗？', '确认删除', { type: 'warning' })
    await deleteRuleConfig(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message)
  }
}

async function confirmEdit() {
  editLoading.value = true
  try {
    if (isAdd.value) {
      await createRuleConfig(editForm.value)
      ElMessage.success('新增成功')
    } else {
      await updateRuleConfig(editForm.value)
      ElMessage.success('更新成功')
    }
    editDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    editLoading.value = false
  }
}

onMounted(fetchData)
</script>
