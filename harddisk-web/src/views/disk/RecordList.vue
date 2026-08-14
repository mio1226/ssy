<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h3>使用记录管理</h3>
      <div>
        <el-button-group style="margin-right: 12px">
          <el-button :type="sortBy === 'id' ? 'primary' : 'default'" size="small" @click="toggleSort('id')">按ID排序</el-button>
          <el-button :type="sortBy === 'disk_id' ? 'primary' : 'default'" size="small" @click="toggleSort('disk_id')">按磁盘ID排序</el-button>
        </el-button-group>
        <el-button type="success" @click="handleExport" :loading="exportLoading">导出到飞书</el-button>
      </div>
    </div>
    <el-card>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="记录ID">
          <el-input v-model="query.recordId" placeholder="精确匹配" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="硬盘型号">
          <el-input v-model="query.model" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="SN码">
          <el-input v-model="query.sn" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operatorName" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="存储内容">
          <el-input v-model="query.storageContent" placeholder="模糊搜索" clearable style="width: 140px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="出库" :value="1" />
            <el-option label="入库待备份" :value="3" />
            <el-option label="入库已备份" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="displaySeq" label="序号" width="80" />
        <el-table-column prop="diskDisplaySeq" label="硬盘序号" width="90" />
        <el-table-column prop="diskModel" label="硬盘型号" min-width="120" />
        <el-table-column prop="diskSn" label="SN码" min-width="160" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outTime" label="出库时间" width="160" />
        <el-table-column prop="inTime" label="入库时间" width="160" />
        <el-table-column prop="storageContent" label="存储内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="parentRecordId" label="父记录ID" width="90" />
        <el-table-column v-if="userStore.isAdmin()" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-if="userStore.isAdmin()" v-model="editDialog" title="编辑使用记录" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="记录ID">{{ editForm.id }}</el-form-item>
        <el-form-item label="硬盘">{{ editForm.diskModel }} ({{ editForm.diskSn }})</el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status">
            <el-option label="出库" :value="1" />
            <el-option label="入库待备份" :value="3" />
            <el-option label="入库已备份" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库时间">
          <el-date-picker v-model="editForm.outTime" type="datetime" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker v-model="editForm.inTime" type="datetime" style="width: 100%" />
        </el-form-item>
        <el-form-item label="存储内容">
          <el-input v-model="editForm.storageContent" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listAllRecords, updateRecord, deleteRecord } from '@/api/disk'
import { checkFeishuConfig, exportRecords } from '@/api/feishu'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = reactive({ recordId: null, model: null, sn: null, operatorName: null, storageContent: null, status: null })
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

const editDialog = ref(false)
const editLoading = ref(false)
const editForm = reactive({ id: null, diskId: null, diskModel: '', diskSn: '', status: 1, outTime: null, inTime: null, storageContent: '' })

const exportLoading = ref(false)

function statusType(s) { return { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success' }[s] || 'info' }
function statusLabel(s) { return { 1: '出库',3: '入库待备份', 4: '入库已备份' }[s] || '未知' }

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (query.recordId) params.recordId = query.recordId
    if (query.model) params.model = query.model
    if (query.sn) params.sn = query.sn
    if (query.operatorName) params.operatorName = query.operatorName
    if (query.storageContent) params.storageContent = query.storageContent
    if (query.status !== null && query.status !== '') params.status = query.status
    if (sortBy.value) { params.sortBy = sortBy.value; params.sortOrder = sortOrder.value }
    const res = await listAllRecords(params)
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchData() }
function resetQuery() { query.recordId = null; query.model = null; query.sn = null; query.operatorName = null; query.storageContent = null; query.status = null; handleSearch() }

function handleEdit(row) {
  Object.assign(editForm, {
    id: row.id, diskId: row.diskId, diskModel: row.diskModel, diskSn: row.diskSn,
    status: row.status, outTime: row.outTime, inTime: row.inTime, storageContent: row.storageContent
  })
  editDialog.value = true
}

async function confirmEdit() {
  editLoading.value = true
  try {
    await updateRecord(editForm.id, {
      status: editForm.status,
      outTime: editForm.outTime,
      inTime: editForm.inTime,
      storageContent: editForm.storageContent
    })
    ElMessage.success('更新成功')
    editDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    editLoading.value = false
  }
}

function handleDelete(row) {
  ElMessageBox.confirm('确认删除该使用记录？', '提示').then(async () => {
    try {
      await deleteRecord(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

async function handleExport() {
  try {
    const configRes = await checkFeishuConfig()
    if (!configRes.data) {
      ElMessage.warning('飞书配置未完善，请先在规则配置中设置飞书参数')
      return
    }
    await ElMessageBox.confirm('确认将使用记录导出到飞书电子表格？', '导出确认', { type: 'info' })
    exportLoading.value = true
    await exportRecords('Sheet1')
    ElMessage.success('导出成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(fetchData)
</script>