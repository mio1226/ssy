<template>
  <div>
    <h3 style="margin-bottom: 16px">违规记录</h3>
    <el-card>
      <div style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center">
        <el-form :inline="true" :model="query" style="margin: 0">
          <el-form-item label="类型">
            <el-select v-model="query.type" placeholder="全部" clearable style="width: 180px">
              <el-option label="全部" value="" />
              <el-option label="出库超时" value="timeout" />
              <el-option label="重复使用" value="reuse" />
              <el-option label="删除活跃硬盘" value="delete_disk_active" />
              <el-option label="违规删除记录" value="delete_record" />
              <el-option label="违规入库" value="inbound_invalid_status" />
              <el-option label="数据不完整" value="incomplete_data" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
              <el-option label="待处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="warning" @click="handleRefresh" :loading="refreshLoading">更新违规记录</el-button>
      </div>
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="diskDisplaySeq" label="硬盘序号" width="90" />
        <el-table-column prop="recordDisplaySeq" label="记录序号" width="90" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'">{{ row.status === 0 ? '待处理' : '已处理' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" @click="handleResolve(row)">标记处理</el-button>
            <el-tag v-else type="info">已处理</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 16px; display: flex; justify-content: center">
        <el-pagination v-model:current-page="page" v-model:page-size="pageSize" :total="total" layout="prev, pager, next, total" @current-change="fetchData" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listViolations, resolveViolation, refreshViolations } from '@/api/rule'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const refreshLoading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = reactive({ type: '', status: null })

function typeLabel(type) {
  const map = {
    timeout: '出库超时',
    reuse: '重复使用',
    delete_disk_active: '删除活跃硬盘',
    delete_record: '违规删除记录',
    inbound_invalid_status: '违规入库',
    incomplete_data: '数据不完整'
  }
  return map[type] || type
}

function typeTag(type) {
  const map = {
    timeout: 'warning',
    reuse: 'danger',
    delete_disk_active: 'danger',
    delete_record: 'danger',
    inbound_invalid_status: 'warning',
    incomplete_data: 'warning'
  }
  return map[type] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (query.type) params.type = query.type
    if (query.status !== null && query.status !== '') params.status = query.status
    const res = await listViolations(params)
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchData() }
function resetQuery() { query.type = ''; query.status = null; handleSearch() }

async function handleResolve(row) {
  try {
    await resolveViolation(row.id)
    ElMessage.success('已标记处理')
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function handleRefresh() {
  refreshLoading.value = true
  try {
    const res = await refreshViolations()
    ElMessage.success(res.data)
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    refreshLoading.value = false
  }
}

onMounted(fetchData)
</script>
