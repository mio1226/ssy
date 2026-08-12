<template>
  <div>
    <h3 style="margin-bottom: 16px">仪表盘</h3>

    <el-row :gutter="16" style="margin-bottom: 16px">
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">硬盘总数</div>
            <div class="stat-value">{{ stats.totalDisks }}</div>
          </div>
          <div class="stat-sub">
            空闲 <span class="stat-num green">{{ stats.idleDisks }}</span>
            &nbsp;/&nbsp; 使用中 <span class="stat-num orange">{{ stats.inUseDisks }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">本月出库</div>
            <div class="stat-value blue">{{ stats.monthOutboundCount }}</div>
          </div>
          <div class="stat-sub">
            本月入库 <span class="stat-num green">{{ stats.monthInboundCount }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">待处理违规</div>
            <div class="stat-value red">{{ stats.pendingViolations }}</div>
          </div>
          <div class="stat-sub">
            总违规 <span class="stat-num">{{ stats.totalViolations }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>硬盘状态分布</span></template>
          <div style="display: flex; align-items: center; justify-content: center; gap: 32px; padding: 16px 0">
            <div style="text-align: center">
              <div style="font-size: 40px; font-weight: bold; color: #67c23a">{{ idlePercent }}%</div>
              <div style="color: #909399; font-size: 13px; margin-top: 4px">空闲率</div>
            </div>
            <div style="width: 1px; height: 60px; background: #e4e7ed"></div>
            <div style="text-align: center">
              <div style="font-size: 40px; font-weight: bold; color: #e6a23c">{{ inUsePercent }}%</div>
              <div style="color: #909399; font-size: 13px; margin-top: 4px">使用率</div>
            </div>
          </div>
          <el-progress :percentage="idlePercent" color="#67c23a" :stroke-width="16" style="margin-bottom: 12px">
            <span>空闲 {{ stats.idleDisks }} 块</span>
          </el-progress>
          <el-progress :percentage="inUsePercent" color="#e6a23c" :stroke-width="16">
            <span>使用中 {{ stats.inUseDisks }} 块</span>
          </el-progress>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header><span>使用记录状态分布</span></template>
          <div style="padding: 8px 0">
            <div class="record-bar">
              <span class="record-label">出库中</span>
              <span class="record-num">{{ stats.outboundRecords }}</span>
            </div>
            <div class="record-bar">
              <span class="record-label">存储数据中</span>
              <span class="record-num">{{ stats.storingRecords }}</span>
            </div>
            <div class="record-bar">
              <span class="record-label">入库待备份</span>
              <span class="record-num">{{ stats.inboundPendingRecords }}</span>
            </div>
            <div class="record-bar">
              <span class="record-label">入库已备份</span>
              <span class="record-num">{{ stats.inboundDoneRecords }}</span>
            </div>
            <div class="record-bar" style="border-bottom: none; font-weight: bold">
              <span class="record-label">总计</span>
              <span class="record-num">{{ stats.totalRecords }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDashboardStats } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

const stats = ref({
  totalDisks: 0, idleDisks: 0, inUseDisks: 0,
  totalRecords: 0, outboundRecords: 0, storingRecords: 0,
  inboundPendingRecords: 0, inboundDoneRecords: 0,
  monthOutboundCount: 0, monthInboundCount: 0,
  totalViolations: 0, pendingViolations: 0
})

const idlePercent = computed(() => {
  if (!stats.value.totalDisks) return 0
  return Math.round((stats.value.idleDisks / stats.value.totalDisks) * 100)
})

const inUsePercent = computed(() => {
  if (!stats.value.totalDisks) return 0
  return Math.round((stats.value.inUseDisks / stats.value.totalDisks) * 100)
})

async function fetchStats() {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch (e) {
    ElMessage.error('加载仪表盘数据失败: ' + e.message)
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.stat-item {
  text-align: center;
  padding: 8px 0;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 48px;
  font-weight: bold;
  color: #303133;
  line-height: 1.1;
}
.stat-value.blue { color: #409eff; }
.stat-value.red { color: #f56c6c; }
.stat-sub {
  text-align: center;
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
}
.stat-num { font-weight: bold; color: #303133; }
.stat-num.green { color: #67c23a; }
.stat-num.orange { color: #e6a23c; }
.record-bar {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.record-label {
  color: #606266;
  font-size: 14px;
}
.record-num {
  font-weight: bold;
  color: #303133;
}
</style>
