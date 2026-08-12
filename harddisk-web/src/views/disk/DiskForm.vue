<template>
  <div>
    <h3 style="margin-bottom: 16px">{{ isEdit ? '编辑硬盘' : '新增硬盘' }}</h3>
    <el-card>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="140px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="硬盘型号" prop="model">
              <el-input v-model="form.model" placeholder="如：捷移、西数SSD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SN码" prop="sn">
              <el-input v-model="form.sn" placeholder="拍照识别或手动输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="容量(TB)" prop="capacity">
              <el-input-number v-model="form.capacity" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="位置">
              <el-input v-model="form.location" placeholder="具体地点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购时间">
              <el-date-picker v-model="form.purchaseTime" type="datetime" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购单价(元)">
              <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="采购OA流程编号">
          <el-input v-model="form.purchaseOaNo" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="loading">{{ isEdit ? '保存' : '创建' }}</el-button>
          <el-button @click="$router.push('/disk')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDisk, createDisk, updateDisk } from '@/api/disk'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  model: '', sn: '', capacity: 0, location: '', purchaseTime: null,
  purchasePrice: 0, purchaseOaNo: '', remark: ''
})

const rules = {
  model: [{ required: true, message: '请输入硬盘型号' }],
  sn: [{ required: true, message: '请输入SN码' }],
  capacity: [{ required: true, message: '请输入容量' }]
}

onMounted(async () => {
  if (isEdit.value) {
    try {
      const res = await getDisk(route.params.id)
      Object.assign(form, res.data)
    } catch (e) {
      ElMessage.error('获取硬盘信息失败')
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => {})
  if (!valid) return
  loading.value = true
  try {
    if (isEdit.value) {
      await updateDisk(route.params.id, form)
      ElMessage.success('更新成功')
    } else {
      await createDisk(form)
      ElMessage.success('创建成功，硬盘已自动入库')
    }
    router.push('/disk')
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    loading.value = false
  }
}
</script>
