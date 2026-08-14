<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h3>硬盘管理</h3>
      <div>
        <el-button-group style="margin-right: 12px">
          <el-button :type="sortBy === 'id' ? 'primary' : 'default'" size="small" @click="toggleSort('id')">按ID排序</el-button>
          <el-button :type="sortBy === 'model' ? 'primary' : 'default'" size="small" @click="toggleSort('model')">按型号排序</el-button>
        </el-button-group>
        <el-button type="primary" @click="$router.push('/disk/create')">新增硬盘</el-button>
      </div>
    </div>
    <el-card>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="型号">
          <el-input v-model="query.model" placeholder="搜索型号" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="SN码">
          <el-input v-model="query.sn" placeholder="搜索SN码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.isIdle" placeholder="全部" clearable style="width: 140px">
            <el-option label="空闲" :value="true" />
            <el-option label="使用中" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="displaySeq" label="序号" width="70" />
        <el-table-column prop="model" label="型号" min-width="120" />
        <el-table-column prop="sn" label="SN码" min-width="160" />
        <el-table-column prop="capacity" label="容量(TB)" width="100" />
        <el-table-column prop="location" label="位置" min-width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isIdle ? 'success' : 'warning'">{{ row.isIdle ? '空闲' : '使用中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purchaseTime" label="采购时间" width="160" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" :width="userStore.isAdmin() ? 280 : 160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/disk/${row.id}/records`)">记录</el-button>
            <el-button size="small" @click="handleOutbound(row)">出库</el-button>
            <el-button v-if="userStore.isAdmin()" size="small" @click="$router.push(`/disk/${row.id}/edit`)">编辑</el-button>
            <el-button v-if="userStore.isAdmin()" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: center">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="outDialog" title="硬盘出库" width="500px">
      <el-form :model="outForm" :rules="outRules" ref="outFormRef" label-width="100px">
        <el-form-item label="硬盘">{{ currentDisk?.model }} ({{ currentDisk?.sn }})</el-form-item>
        <el-form-item label="存储内容" prop="storageContent">
          <el-input v-model="outForm.storageContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="出库时间" prop="outTime">
          <el-date-picker v-model="outForm.outTime" type="datetime" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmOutbound" :loading="outLoading">确认出库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listDisks, deleteDisk, outbound } from '@/api/disk'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = reactive({ model: '', sn: '', isIdle: null })
const sortBy = ref('')
const sortOrder = ref('desc')

function toggleSort(field) {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = field
    sortOrder.value = 'asc'
  }
  fetchData()
}

const outDialog = ref(false)
const outLoading = ref(false)
const currentDisk = ref(null)
const outForm = reactive({ diskId: null, storageContent: '', outTime: null })
const outRules = { storageContent: [{ required: true, message: '请输入存储内容' }] }
const outFormRef = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value, ...query }
    if (sortBy.value) { params.sortBy = sortBy.value; params.sortOrder = sortOrder.value }
    const res = await listDisks(params)
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchData() }
function resetQuery() { query.model = ''; query.sn = ''; query.isIdle = null; handleSearch() }

function handleOutbound(row) {
  if (!row.isIdle) { ElMessage.warning('该硬盘当前不可用'); return }
  currentDisk.value = row
  outForm.diskId = row.id
  outForm.storageContent = ''
  outForm.outTime = null
  outDialog.value = true
}

async function confirmOutbound() {
  const valid = await outFormRef.value.validate().catch(() => {})
  if (!valid) return
  outLoading.value = true
  try {
    await outbound(outForm)
    ElMessage.success('出库成功')
    outDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    outLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该硬盘？', '提示').then(async () => {
    try {
      await deleteDisk(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

onMounted(fetchData)
</script>