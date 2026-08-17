<template>
  <el-dialog v-model="visible" title="上传照片识别 SN 码" width="520px" @close="handleClose">
    <div v-if="!recognized" class="scanner-body">
      <el-upload
        ref="uploadRef"
        drag
        accept="image/*"
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        :key="uploadKey"
      >
        <div v-if="!previewUrl" class="upload-placeholder">
          <el-icon :size="48" style="color: #c0c4cc; margin-bottom: 12px">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </el-icon>
          <div class="upload-text">点击或拖拽硬盘标签照片到这里</div>
          <div class="upload-hint">支持 JPG / PNG 格式，建议条码区域清晰可见</div>
        </div>
        <div v-else class="preview-area">
          <img :src="previewUrl" alt="preview" class="preview-img" />
        </div>
      </el-upload>

      <div v-if="previewUrl" style="margin-top: 12px; text-align: center">
        <el-button type="primary" @click="startRecognition" :loading="recognizing">
          {{ recognizing ? '识别中...' : '开始识别' }}
        </el-button>
        <el-button @click="clearImage">重新选择</el-button>
      </div>

      <div v-if="recognizing" style="margin-top: 16px; text-align: center">
        <el-progress :percentage="progress" :stroke-width="6" />
        <p style="color: #909399; font-size: 13px; margin-top: 8px">{{ statusText }}</p>
      </div>
    </div>

    <div v-else class="result-area">
      <div style="margin-bottom: 12px">
        <img :src="previewUrl" alt="preview" class="preview-img-small" />
      </div>
      <el-form label-width="80px">
        <el-form-item label="识别结果">
          <el-input v-model="snResult" placeholder="可手动修改" size="large" />
        </el-form-item>
        <el-form-item label="条码类型">
          <el-tag type="info">{{ barcodeFormat || '手动输入' }}</el-tag>
        </el-form-item>
      </el-form>
      <div style="text-align: center; margin-top: 16px">
        <el-button type="primary" @click="confirmResult">确认使用</el-button>
        <el-button @click="retryRecognition">重新识别</el-button>
        <el-button @click="handleClose">取消</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { BrowserMultiFormatReader } from '@zxing/library'
import { ElMessage } from 'element-plus'

const emit = defineEmits(['confirm'])

const visible = ref(false)
const previewUrl = ref('')
const recognized = ref(false)
const recognizing = ref(false)
const progress = ref(0)
const statusText = ref('')
const snResult = ref('')
const barcodeFormat = ref('')
const uploadRef = ref(null)
const uploadKey = ref(0)
let selectedFile = null

function open() {
  visible.value = true
  resetState()
}

defineExpose({ open })

function handleClose() {
  visible.value = false
  resetState()
}

function handleFileChange(file) {
  selectedFile = file.raw
  previewUrl.value = URL.createObjectURL(file.raw)
}

function resetState() {
  // 清理预览 URL
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
  }
  previewUrl.value = ''
  selectedFile = null
  recognized.value = false
  recognizing.value = false
  progress.value = 0
  snResult.value = ''
  barcodeFormat.value = ''
  statusText.value = ''

  // 重置 el-upload 内部状态
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  // 递增 key 强制重新渲染 el-upload 组件
  uploadKey.value++
}

function clearImage() {
  resetState()
}

async function decodeBarcodeFromImage(imageUrl) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = async () => {
      try {
        const reader = new BrowserMultiFormatReader()
        const result = await reader.decodeFromImageElement(img)
        resolve(result)
      } catch (e) {
        try {
          const canvas = document.createElement('canvas')
          const ctx = canvas.getContext('2d')
          canvas.width = img.width
          canvas.height = img.height
          ctx.drawImage(img, 0, 0)
          const reader = new BrowserMultiFormatReader()
          const result = await reader.decodeFromCanvas(canvas)
          resolve(result)
        } catch (e2) {
          reject(e2)
        }
      }
    }
    img.onerror = () => reject(new Error('图片加载失败'))
    img.src = imageUrl
  })
}

async function startRecognition() {
  if (!selectedFile) return
  recognizing.value = true
  progress.value = 0
  statusText.value = '正在解码条码...'
  try {
    const imageUrl = URL.createObjectURL(selectedFile)
    const result = await decodeBarcodeFromImage(imageUrl)
    URL.revokeObjectURL(imageUrl)

    const rawText = result.getText()
    const cleaned = rawText.replace(/[^A-Za-z0-9\-]/g, '').toUpperCase()
    snResult.value = cleaned || rawText
    barcodeFormat.value = result.getBarcodeFormat()
    progress.value = 100
    statusText.value = '识别完成'
    recognized.value = true
  } catch (e) {
    ElMessage.warning('未识别到条码，请确保照片中条码清晰可见')
    clearImage()
  } finally {
    recognizing.value = false
  }
}

function retryRecognition() {
  recognized.value = false
  snResult.value = ''
  barcodeFormat.value = ''
  nextTick(() => startRecognition())
}

function confirmResult() {
  emit('confirm', snResult.value.trim())
  visible.value = false
  resetState()
}
</script>

<style scoped>
.scanner-body, .result-area {
  min-height: 200px;
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}
.upload-text {
  font-size: 14px;
  color: #606266;
}
.upload-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
.preview-area {
  display: flex;
  justify-content: center;
}
.preview-img {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 4px;
}
.preview-img-small {
  max-width: 200px;
  max-height: 120px;
  object-fit: contain;
  border-radius: 4px;
  display: block;
  margin: 0 auto;
}
</style>