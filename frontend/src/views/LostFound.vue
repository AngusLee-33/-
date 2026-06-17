<template>
  <main class="lost-found-page">
    <section class="page-header">
      <div>
        <span>校园互助</span>
        <h1>失物招领</h1>
        <p>发布丢失物品或拾到物品信息，让校园里的小麻烦更快被解决。</p>
      </div>
      <el-button type="primary" @click="openCreateDialog('lost')">发布信息</el-button>
    </section>

    <section class="toolbar">
      <el-segmented v-model="filterType" :options="typeOptions" @change="loadItems" />
      <el-input v-model.trim="keyword" placeholder="搜索物品、地点或描述" clearable />
    </section>

    <section class="items-grid" v-loading="loading">
      <el-empty v-if="!filteredItems.length" description="暂无失物招领信息" />
      <article v-for="item in filteredItems" v-else :key="item.lostFoundId" class="item-card">
        <div v-if="getCoverImage(item)" class="item-cover">
          <img :src="getCoverImage(item)" alt="失物招领图片" />
        </div>
        <div class="item-top">
          <el-tag :type="item.type === 'lost' ? 'danger' : 'success'" effect="light">
            {{ getTypeText(item.type) }}
          </el-tag>
          <el-tag :type="getStatusType(item.status)" effect="plain">{{ getStatusText(item.status) }}</el-tag>
        </div>
        <h2>{{ item.title }}</h2>
        <p class="description">{{ item.description }}</p>
        <div class="meta-list">
          <span>地点：{{ item.location }}</span>
          <span>时间：{{ formatTime(item.eventTime || item.createTime) }}</span>
          <span>联系人：{{ item.user?.realName || item.user?.username }}</span>
          <span>联系方式：{{ item.contact }}</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" plain @click="openContactDialog(item)">联系发布人</el-button>
        </div>
      </article>
    </section>

    <el-dialog v-model="showContactModal" title="联系发布人" width="420px">
      <div v-if="selectedItem" class="contact-panel">
        <div class="contact-goods">
          <strong>{{ selectedItem.title }}</strong>
          <span>{{ getTypeText(selectedItem.type) }} · {{ selectedItem.location }}</span>
        </div>
        <div class="contact-row">
          <span>发布人</span>
          <strong>{{ getPublisherName(selectedItem) }}</strong>
        </div>
        <div class="contact-row">
          <span>用户名</span>
          <strong>{{ selectedItem.user?.username || '未提供' }}</strong>
        </div>
        <div class="contact-row">
          <span>联系方式</span>
          <strong>{{ selectedItem.contact || '未提供' }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="showContactModal = false">关闭</el-button>
        <el-button type="success" @click="startChat">在线交流</el-button>
        <el-button type="primary" :disabled="!selectedItem?.contact" @click="copyContact">复制联系方式</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="发布失物招领" width="520px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="类型">
          <el-segmented v-model="createForm.type" :options="publishTypeOptions" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model.trim="createForm.title" maxlength="100" show-word-limit placeholder="例如：丢失黑色校园卡" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model.trim="createForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写清楚外观、特征、拾取/丢失经过等"
          />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model.trim="createForm.location" maxlength="120" placeholder="例如：一食堂二楼、图书馆门口" />
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="createForm.eventTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择丢失或拾到时间"
          />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model.trim="createForm.contact" maxlength="100" placeholder="手机号、微信号或宿舍楼信息" />
        </el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="createForm.images" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitItem">发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getErrorMessage, lostFoundApi } from '../api'
import ImageUploader from '../components/ImageUploader.vue'

const loading = ref(false)
const router = useRouter()
const submitting = ref(false)
const dialogVisible = ref(false)
const showContactModal = ref(false)
const selectedItem = ref(null)
const filterType = ref('')
const keyword = ref('')
const items = ref([])

const typeOptions = [
  { label: '全部', value: '' },
  { label: '寻物', value: 'lost' },
  { label: '招领', value: 'found' }
]

const publishTypeOptions = [
  { label: '我丢了东西', value: 'lost' },
  { label: '我捡到东西', value: 'found' }
]

const createForm = reactive({
  type: 'lost',
  title: '',
  description: '',
  location: '',
  eventTime: '',
  contact: '',
  images: []
})

const filteredItems = computed(() => {
  const text = keyword.value.toLowerCase()
  if (!text) return items.value
  return items.value.filter(item => {
    return [item.title, item.description, item.location, item.contact]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(text))
  })
})

const openCreateDialog = (type) => {
  createForm.type = type
  dialogVisible.value = true
}

const getCoverImage = (item) => {
  return Array.isArray(item.images) && item.images.length ? item.images[0] : ''
}

const loadItems = async () => {
  loading.value = true
  try {
    const response = await lostFoundApi.getAll(filterType.value)
    if (response.code === 200) {
      items.value = response.data || []
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '加载失物招领失败'))
  } finally {
    loading.value = false
  }
}

const submitItem = async () => {
  const message = validateForm()
  if (message) {
    ElMessage.warning(message)
    return
  }

  submitting.value = true
  try {
    const response = await lostFoundApi.create(createForm)
    if (response.code === 200 || response.code === 201) {
      ElMessage.success('发布成功')
      dialogVisible.value = false
      resetForm()
      await loadItems()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发布失败'))
  } finally {
    submitting.value = false
  }
}

const openContactDialog = (item) => {
  selectedItem.value = item
  showContactModal.value = true
}

const getPublisherName = (item) => {
  return item?.user?.realName || item?.user?.username || '未提供'
}

const copyContact = async () => {
  const contact = selectedItem.value?.contact
  if (!contact) {
    ElMessage.warning('暂未提供联系方式')
    return
  }

  try {
    await navigator.clipboard.writeText(contact)
    ElMessage.success('联系方式已复制')
  } catch (error) {
    ElMessage.info(`联系方式：${contact}`)
  }
}

const startChat = () => {
  const publisher = selectedItem.value?.user
  if (!publisher?.userId) {
    ElMessage.warning('发布人信息不完整，暂时无法发起交流')
    return
  }
  router.push({
    path: '/messages',
    query: {
      peerId: publisher.userId,
      peerName: publisher.realName || publisher.username || '发布人',
      targetType: 'lost-found',
      targetId: selectedItem.value.lostFoundId,
      targetTitle: selectedItem.value.title
    }
  })
}

const validateForm = () => {
  if (!createForm.title) return '请输入标题'
  if (!createForm.description) return '请输入描述'
  if (!createForm.location) return '请输入地点'
  if (!createForm.contact) return '请输入联系方式'
  return ''
}

const resetForm = () => {
  createForm.type = 'lost'
  createForm.title = ''
  createForm.description = ''
  createForm.location = ''
  createForm.eventTime = ''
  createForm.contact = ''
  createForm.images = []
}

const getTypeText = (type) => ({
  lost: '寻物',
  found: '招领'
}[type] || type)

const getStatusText = (status) => ({
  open: '进行中',
  resolved: '已解决',
  closed: '已关闭'
}[status] || status)

const getStatusType = (status) => ({
  open: 'primary',
  resolved: 'success',
  closed: 'info'
}[status] || 'info')

const formatTime = (time) => {
  if (!time) return '时间待补充'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadItems)
</script>

<style scoped>
.lost-found-page {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  max-width: 1180px;
  margin: 0 auto 18px;
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.page-header span {
  color: #1f5eff;
  font-size: 13px;
  font-weight: 700;
}

.page-header h1 {
  margin: 6px 0 8px;
  color: #172033;
  font-size: 28px;
  line-height: 1.25;
}

.page-header p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.toolbar {
  display: grid;
  grid-template-columns: auto minmax(220px, 360px);
  gap: 14px;
  align-items: center;
  max-width: 1180px;
  margin: 0 auto 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  max-width: 1180px;
  min-height: 220px;
  margin: 0 auto;
}

.item-card {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.item-cover {
  height: 170px;
  margin-bottom: 14px;
  overflow: hidden;
  background: #eef4ff;
  border-radius: 8px;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.item-card h2 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 19px;
  line-height: 1.35;
}

.description {
  min-height: 48px;
  margin: 0 0 14px;
  color: #4b5563;
  line-height: 1.6;
}

.meta-list {
  display: grid;
  gap: 8px;
  color: #64748b;
  font-size: 14px;
}

.card-actions {
  margin-top: 14px;
}

.contact-panel {
  display: grid;
  gap: 14px;
}

.contact-goods {
  display: grid;
  gap: 6px;
  padding: 14px;
  background: #f6f9fd;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.contact-goods strong {
  color: #172033;
  font-size: 16px;
}

.contact-goods span {
  color: #667085;
  font-size: 13px;
}

.contact-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #edf1f6;
}

.contact-row span {
  color: #667085;
}

.contact-row strong {
  color: #172033;
  text-align: right;
  word-break: break-all;
}

@media (max-width: 640px) {
  .lost-found-page {
    padding: 16px;
  }

  .page-header,
  .toolbar {
    grid-template-columns: 1fr;
  }

  .page-header {
    display: grid;
  }
}
</style>
