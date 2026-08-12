<template>
  <div>
    <h3 style="margin-bottom: 16px">违规记录</h3>
    <el-card>
      <el-form :inline="true" :model="query" style="margin-bottom: 16px">
        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 140px">
            <el-option label="超时" value="timeout" />
            <el-option label="重复使用" value="reuse" />
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
      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 'timeout' ? 'warning' : 'danger'">{{ row.type === 'timeout' ? '超时' : '重复使用' }}</el-tag>
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
import { listViolations, resolveViolation } from '@/api/rule'
import { checkFeishuConfig, exportViolations } from '@/api/feishu'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const query = reactive({ type: null, status: null })

async function fetchData() {
  loading.value = true
  try {
    const res = await listViolations({ page: page.value, pageSize: pageSize.value, ...query })
    list.value = res.data.list
    total.value = res.data.total
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}

function handleSearch() { page.value = 1; fetchData() }
function resetQuery() { query.type = null; query.status = null; handleSearch() }

async function handleResolve(row) {
  try {
    await resolveViolation(row.id)
    ElMessage.success('已标记处理')
    fetchData()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

const exportLoading = ref(false)

async function handleExport() {
  try {
    const configRes = await checkFeishuConfig()
    if (!configRes.data) {
      ElMessage.warning('飞书配置未完善，请先在规则配置中设置飞书参数')
      return
    }
    await ElMessageBox.confirm('确认将违规记录导出到飞书电子表格？', '导出确认', { type: 'info' })
    exportLoading.value = true
    await exportViolations('Sheet1')
    ElMessage.success('导出成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(fetchData)
</script>
