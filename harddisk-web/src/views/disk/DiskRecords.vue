<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h3>使用记录 - {{ disk?.model }} ({{ disk?.sn }})</h3>
      <el-button @click="$router.push('/disk')">返回</el-button>
    </div>
    <el-card>
      <el-table :data="records" border stripe v-loading="loading">
        <el-table-column prop="displaySeq" label="序号" width="80" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outTime" label="出库时间" width="160" />
        <el-table-column prop="inTime" label="入库时间" width="160" />
        <el-table-column prop="storageContent" label="存储内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="parentRecordId" label="父记录ID" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1 || row.status === 3" size="small" type="primary" @click="handleInbound(row)">入库</el-button>
            <el-tag v-else type="info">已完成</el-tag>
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

    <el-dialog v-model="inDialog" title="硬盘入库" width="500px">
      <el-form :model="inForm" ref="inFormRef" label-width="100px">
        <el-form-item label="入库状态">
          <el-select v-model="inForm.status">
            <el-option label="入库待备份" :value="3" />
            <el-option label="入库已备份" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储内容">
          <el-input v-model="inForm.storageContent" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker v-model="inForm.inTime" type="datetime" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmInbound" :loading="inLoading">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDisk, getRecords, inbound } from '@/api/disk'
import { ElMessage } from 'element-plus'

const route = useRoute()
const disk = ref(null)
const records = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const inDialog = ref(false)
const inLoading = ref(false)
const currentRecord = ref(null)
const inForm = ref({ status: 3, storageContent: '', inTime: null })
const inFormRef = ref(null)

function statusType(s) {
  return { 1: 'warning', 2: 'primary', 3: 'info', 4: 'success' }[s] || 'info'
}
function statusLabel(s) {
  return { 1: '出库',3: '入库待备份', 4: '入库已备份' }[s] || '未知'
}

async function fetchData() {
  loading.value = true
  try {
    const [diskRes, recordsRes] = await Promise.all([
      getDisk(route.params.id),
      getRecords(route.params.id, { page: page.value, pageSize: pageSize.value })
    ])
    disk.value = diskRes.data
    records.value = recordsRes.data.list
    total.value = recordsRes.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleInbound(row) {
  currentRecord.value = row
  inForm.value = { status: 3, storageContent: row.storageContent || '', inTime: null }
  inDialog.value = true
}

async function confirmInbound() {
  inLoading.value = true
  try {
    await inbound({ recordId: currentRecord.value.id, ...inForm.value })
    ElMessage.success('入库成功')
    inDialog.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    inLoading.value = false
  }
}

onMounted(fetchData)
</script>