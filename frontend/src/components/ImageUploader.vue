<template>
  <div class="image-uploader">
    <el-upload
      list-type="picture-card"
      :file-list="fileList"
      :http-request="uploadImage"
      :on-remove="removeImage"
      :on-preview="previewImage"
      :before-upload="beforeUpload"
      :limit="limit"
      accept="image/jpeg,image/png,image/gif,image/webp"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
    <p class="upload-tip">最多上传 {{ limit }} 张，支持 jpg、png、gif、webp，单张不超过 10MB。</p>

    <el-dialog v-model="previewVisible" title="图片预览" width="520px">
      <img class="preview-image" :src="previewUrl" alt="图片预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadApi } from '../api'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  limit: {
    type: Number,
    default: 3
  }
})

const emit = defineEmits(['update:modelValue'])

const previewVisible = ref(false)
const previewUrl = ref('')

const fileList = computed(() => props.modelValue.map((url, index) => ({
  name: `图片${index + 1}`,
  url
})))

const beforeUpload = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!isImage) {
    ElMessage.warning('只能上传 jpg、png、gif、webp 图片')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 10MB')
    return false
  }
  return true
}

const uploadImage = async ({ file, onSuccess, onError }) => {
  try {
    const response = await uploadApi.image(file)
    if (response.code === 200) {
      const url = response.data?.url
      emit('update:modelValue', [...props.modelValue, url])
      ElMessage.success('图片上传成功')
      onSuccess(response)
    } else {
      ElMessage.error(response.message || '图片上传失败')
      onError(new Error(response.message || '图片上传失败'))
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error?.message || '图片上传失败')
    onError(error)
  }
}

const removeImage = (file) => {
  emit('update:modelValue', props.modelValue.filter(url => url !== file.url))
}

const previewImage = (file) => {
  previewUrl.value = file.url
  previewVisible.value = true
}
</script>

<style scoped>
.image-uploader {
  width: 100%;
}

.upload-tip {
  margin: 8px 0 0;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.preview-image {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 8px;
}
</style>
