<template>
  <main class="carpool-container">
    <section class="page-header">
      <div>
        <span>出行互助</span>
        <h1>拼车服务</h1>
      </div>
      <el-button type="primary" @click="showCreateModal = true">发布拼车</el-button>
    </section>

    <section class="search-bar">
      <el-input v-model.trim="searchForm.departure" clearable placeholder="出发地" />
      <el-input v-model.trim="searchForm.destination" clearable placeholder="目的地" />
      <el-button type="primary" @click="search">搜索</el-button>
    </section>

    <section class="carpool-list">
      <el-empty v-if="!carpoolList.length" description="暂无拼车信息" />
      <el-card v-for="item in carpoolList" :key="item.carpoolId" class="item-card">
        <div v-if="getCoverImage(item)" class="item-cover">
          <img :src="getCoverImage(item)" alt="拼车图片" />
        </div>
        <div class="item-header">
          <span class="user-name">{{ item.user?.realName || item.user?.username }}</span>
          <span class="status" :class="item.status">{{ getStatusText(item.status) }}</span>
        </div>
        <div class="route">
          <el-icon><MapLocation /></el-icon>
          <span>{{ item.departure }} → {{ item.destination }}</span>
        </div>
        <div class="info">
          <span>{{ formatTime(item.departureTime) }}</span>
          <span>{{ formatCarpoolPeople(item) }}</span>
          <span class="price">¥{{ item.price }}</span>
        </div>
        <p class="description">{{ item.description }}</p>
        <div class="card-actions">
          <el-button type="primary" plain @click="applyCarpool(item.carpoolId)">申请拼车</el-button>
          <el-button plain @click="openContactDialog(item)">联系发布人</el-button>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="showContactModal" title="联系发布人" width="420px">
      <div v-if="selectedItem" class="contact-panel">
        <div class="contact-goods">
          <strong>{{ selectedItem.departure }} → {{ selectedItem.destination }}</strong>
          <span>{{ formatTime(selectedItem.departureTime) }} · {{ selectedItem.seats }} 个座位 · ¥{{ selectedItem.price }}</span>
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
          <span>手机号</span>
          <strong>{{ selectedItem.user?.phone || '发布人暂未设置手机号' }}</strong>
        </div>
        <el-alert
          v-if="!selectedItem.user?.phone"
          title="发布人还没有在个人中心填写手机号，可以先使用在线交流。"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
      <template #footer>
        <el-button @click="showContactModal = false">关闭</el-button>
        <el-button type="success" @click="startChat">在线交流</el-button>
        <el-button type="primary" :disabled="!selectedItem?.user?.phone" @click="copyPublisherPhone">复制手机号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateModal" title="发布拼车">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="出发地">
          <el-input v-model.trim="createForm.departure" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="目的地">
          <el-input v-model.trim="createForm.destination" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="出发时间">
          <el-date-picker
            v-model="createForm.departureTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="请选择出发时间"
          />
        </el-form-item>
        <el-form-item label="座位数">
          <el-input v-model.number="createForm.seats" type="number" min="1" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model.number="createForm.price" type="number" min="0.01" step="0.01" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model.trim="createForm.description"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="可填写集合地点、联系方式或备注"
          />
        </el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="createForm.images" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateModal = false">取消</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="createCarpool">发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { carpoolApi, getErrorMessage } from '../api'
import { ElMessage } from 'element-plus'
import { MapLocation } from '@element-plus/icons-vue'
import ImageUploader from '../components/ImageUploader.vue'

const carpoolList = ref([])
const router = useRouter()
const showCreateModal = ref(false)
const showContactModal = ref(false)
const selectedItem = ref(null)
const isSubmitting = ref(false)

const searchForm = reactive({
  departure: '',
  destination: ''
})

const createForm = reactive({
  departure: '',
  destination: '',
  departureTime: null,
  seats: 4,
  price: 0,
  description: '',
  images: []
})

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    active: '招募中',
    confirmed: '已确认',
    completed: '已完成',
    canceled: '已取消'
  }
  return map[status] || status
}

const formatTime = (time) => {
  return new Date(time).toLocaleString('zh-CN')
}

const formatCarpoolPeople = (item) => {
  const seats = Number(item?.seats)
  const acceptedCount = Number(item?.acceptedCount)
  const remainingSeats = Number.isFinite(seats) && seats >= 0 ? Math.floor(seats) : 0
  const joinedCount = Number.isFinite(acceptedCount) && acceptedCount >= 0 ? Math.floor(acceptedCount) : 0
  return `已拼${joinedCount}人 / 剩余${remainingSeats}座`
}

const getCoverImage = (item) => {
  return Array.isArray(item.images) && item.images.length ? item.images[0] : ''
}

const getPublisherName = (item) => {
  return item?.user?.realName || item?.user?.username || '未提供'
}

const search = async () => {
  const response = await carpoolApi.getAll()
  if (response.code === 200) {
    let data = response.data
    if (searchForm.departure) {
      data = data.filter(item => item.departure.includes(searchForm.departure))
    }
    if (searchForm.destination) {
      data = data.filter(item => item.destination.includes(searchForm.destination))
    }
    carpoolList.value = data
  }
}

const applyCarpool = async (id) => {
  try {
    const response = await carpoolApi.apply(id)
    if (response.code === 200) {
      ElMessage.success('申请成功')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申请失败'))
  }
}

const createCarpool = async () => {
  const validationMessage = validateCreateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  isSubmitting.value = true
  try {
    const response = await carpoolApi.create(createForm)
    if (response.code === 200) {
      ElMessage.success('发布成功')
      showCreateModal.value = false
      resetCreateForm()
      await loadCarpools()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发布失败'))
  } finally {
    isSubmitting.value = false
  }
}

const loadCarpools = async () => {
  const response = await carpoolApi.getAll()
  if (response.code === 200) {
    carpoolList.value = response.data
  }
}

const validateCreateForm = () => {
  if (!createForm.departure) {
    return '请输入出发地'
  }
  if (!createForm.destination) {
    return '请输入目的地'
  }
  if (createForm.departure === createForm.destination) {
    return '出发地和目的地不能相同'
  }
  if (!createForm.departureTime) {
    return '请选择出发时间'
  }
  if (new Date(createForm.departureTime).getTime() <= Date.now()) {
    return '出发时间必须晚于当前时间'
  }
  if (!Number.isInteger(createForm.seats) || createForm.seats <= 0) {
    return '座位数必须为正整数'
  }
  if (Number(createForm.price) <= 0) {
    return '价格必须大于 0'
  }
  return ''
}

const resetCreateForm = () => {
  createForm.departure = ''
  createForm.destination = ''
  createForm.departureTime = null
  createForm.seats = 4
  createForm.price = 0
  createForm.description = ''
  createForm.images = []
}

const openContactDialog = (item) => {
  selectedItem.value = item
  showContactModal.value = true
}

const copyPublisherPhone = async () => {
  const phone = selectedItem.value?.user?.phone
  if (!phone) {
    ElMessage.warning('发布人暂未设置手机号')
    return
  }

  try {
    await navigator.clipboard.writeText(phone)
    ElMessage.success('手机号已复制')
  } catch (error) {
    ElMessage.info(`发布人手机号：${phone}`)
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
      targetType: 'carpool',
      targetId: selectedItem.value.carpoolId,
      targetTitle: `${selectedItem.value.departure} → ${selectedItem.value.destination}`
    }
  })
}

onMounted(loadCarpools)
</script>

<style scoped>
.carpool-container {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header,
.search-bar,
.carpool-list {
  max-width: 1180px;
  margin-left: auto;
  margin-right: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 18px;
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
  margin: 6px 0 0;
  color: #172033;
  font-size: 28px;
  line-height: 1.25;
}

.search-bar {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) minmax(160px, 1fr) auto;
  gap: 12px;
  margin-bottom: 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.carpool-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 16px;
  min-height: 220px;
}

.item-card {
  border-radius: 8px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.item-cover {
  height: 160px;
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

.item-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(31, 45, 61, 0.1);
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.user-name {
  color: #172033;
  font-weight: 700;
}

.status {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
}

.status.pending {
  background: #fff3cd;
  color: #856404;
}

.status.confirmed {
  background: #d4edda;
  color: #155724;
}

.status.active {
  background: #e7f7f5;
  color: #04756f;
}

.status.canceled {
  background: #fde2e2;
  color: #b42318;
}

.route {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #1f5eff;
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}

.info {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
  min-height: 40px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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
  .carpool-container {
    padding: 16px;
  }

  .page-header,
  .search-bar {
    grid-template-columns: 1fr;
  }

  .page-header {
    display: grid;
  }
}
</style>
