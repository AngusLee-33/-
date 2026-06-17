<template>
  <main class="parttime-container">
    <section class="page-header">
      <div>
        <span>灵活岗位</span>
        <h1>兼职招聘</h1>
      </div>
      <el-button type="primary" @click="showCreateModal = true">发布兼职</el-button>
    </section>

    <section class="search-bar">
      <el-input v-model.trim="searchForm.keyword" clearable placeholder="搜索岗位、描述或地点" />
      <el-button type="primary" @click="search">搜索</el-button>
    </section>

    <section class="parttime-list">
      <el-empty v-if="!partTimeList.length" description="暂无兼职信息" />
      <el-card v-for="item in partTimeList" :key="item.partTimeId" class="item-card">
        <div v-if="getCoverImage(item)" class="item-cover">
          <img :src="getCoverImage(item)" alt="兼职图片" />
        </div>
        <div class="card-top">
          <h3>{{ item.title }}</h3>
          <span class="status" :class="getDisplayStatus(item)">{{ getStatusText(item) }}</span>
        </div>
        <p class="description">{{ item.description }}</p>
        <div class="info">
          <span class="salary">¥{{ item.salaryMin }}-{{ item.salaryMax }}/小时</span>
          <span>{{ item.workTime }}</span>
          <span>招聘人数：{{ getAcceptedCount(item) }}/{{ getRecruitCount(item) }}人</span>
        </div>
        <div class="extra-info">
          <span v-if="item.location">地点：{{ item.location }}</span>
          <span v-else>地点待定</span>
        </div>
        <p v-if="item.requirements" class="requirements">要求：{{ item.requirements }}</p>
        <div class="card-actions">
          <el-button v-if="!isMerchant" type="primary" plain :disabled="!canApplyPartTime(item)" @click="applyPartTime(item)">
            {{ getApplyButtonText(item) }}
          </el-button>
          <el-button
            v-if="isMerchant && isOwnPartTime(item) && item.status !== 'closed'"
            type="danger"
            plain
            @click="closePartTime(item)"
          >
            下架招聘
          </el-button>
          <el-button plain @click="openContactDialog(item)">联系商家</el-button>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="showContactModal" title="联系商家" width="420px">
      <div v-if="selectedItem" class="contact-panel">
        <div class="contact-goods">
          <strong>{{ selectedItem.title }}</strong>
          <span>{{ selectedItem.workTime }} · {{ selectedItem.location || '地点待定' }} · 招聘{{ getRecruitCount(selectedItem) }}人</span>
        </div>
        <div class="contact-row">
          <span>商家</span>
          <strong>{{ getMerchantName(selectedItem) }}</strong>
        </div>
        <div class="contact-row">
          <span>用户名</span>
          <strong>{{ selectedItem.merchant?.username || '未提供' }}</strong>
        </div>
        <div class="contact-row">
          <span>手机号</span>
          <strong>{{ selectedItem.merchant?.phone || '商家暂未设置手机号' }}</strong>
        </div>
        <el-alert
          v-if="!selectedItem.merchant?.phone"
          title="商家还没有在个人中心填写手机号，可以先使用在线交流。"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
      <template #footer>
        <el-button @click="showContactModal = false">关闭</el-button>
        <el-button type="success" @click="startChat">在线交流</el-button>
        <el-button type="primary" :disabled="!selectedItem?.merchant?.phone" @click="copyMerchantPhone">复制手机号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateModal" title="发布兼职">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model.trim="createForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model.trim="createForm.description"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请说明工作内容、时间安排和结算方式"
          />
        </el-form-item>
        <el-form-item label="最低薪资">
          <el-input v-model.number="createForm.salaryMin" type="number" min="0.01" step="0.01" />
        </el-form-item>
        <el-form-item label="最高薪资">
          <el-input v-model.number="createForm.salaryMax" type="number" min="0.01" step="0.01" />
        </el-form-item>
        <el-form-item label="招聘人数">
          <el-input-number v-model="createForm.recruitCount" :min="1" :max="999" :step="1" step-strictly />
        </el-form-item>
        <el-form-item label="工作时间">
          <el-input v-model.trim="createForm.workTime" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="工作地点">
          <el-input v-model.trim="createForm.location" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="要求">
          <el-input
            v-model.trim="createForm.requirements"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="可填写技能、经验、到岗时间等要求"
          />
        </el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="createForm.images" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateModal = false">取消</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="createPartTime">发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getErrorMessage, partTimeApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUploader from '../components/ImageUploader.vue'

const partTimeList = ref([])
const router = useRouter()
const userStore = useUserStore()
const showCreateModal = ref(false)
const showContactModal = ref(false)
const selectedItem = ref(null)
const isSubmitting = ref(false)

const searchForm = reactive({
  keyword: ''
})

const createForm = reactive({
  title: '',
  description: '',
  salaryMin: 0,
  salaryMax: 0,
  recruitCount: 1,
  workTime: '',
  location: '',
  requirements: '',
  images: []
})

const getDisplayStatus = (item) => {
  if (isRecruitmentFull(item)) return 'closed'
  return item?.status || ''
}

const getStatusText = (item) => {
  if (isRecruitmentFull(item)) return '已招满'
  const map = {
    pending: '待审核',
    active: '招募中',
    closed: '已关闭'
  }
  return map[item?.status] || item?.status
}

const getCoverImage = (item) => {
  return Array.isArray(item.images) && item.images.length ? item.images[0] : ''
}

const getMerchantName = (item) => {
  return item?.merchant?.realName || item?.merchant?.username || '未提供'
}

const getRecruitCount = (item) => {
  const count = Number(item?.recruitCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 1
}

const isMerchant = computed(() => userStore.user?.role === 'merchant')

const getAcceptedCount = (item) => {
  const count = Number(item?.acceptedCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

const isRecruitmentFull = (item) => {
  return getAcceptedCount(item) >= getRecruitCount(item)
}

const isOwnPartTime = (item) => {
  return Boolean(userStore.user?.userId && item?.merchant?.userId === userStore.user.userId)
}

const canApplyPartTime = (item) => {
  return !isOwnPartTime(item) && item?.status === 'active' && !isRecruitmentFull(item)
}

const getApplyButtonText = (item) => {
  if (isOwnPartTime(item)) return '自己发布'
  if (isRecruitmentFull(item)) return '已招满'
  if (item?.status === 'closed') return '已关闭'
  if (item?.status !== 'active') return '待审核'
  return '申请兼职'
}

const search = async () => {
  const response = isMerchant.value ? await partTimeApi.getMy() : await partTimeApi.getAll()
  if (response.code === 200) {
    let data = response.data
    if (searchForm.keyword) {
      data = data.filter(item => item.title.includes(searchForm.keyword) || 
                                 item.description.includes(searchForm.keyword))
    }
    partTimeList.value = data
  }
}

const applyPartTime = async (item) => {
  if (isOwnPartTime(item)) {
    ElMessage.warning('不能申请自己发布的兼职')
    return
  }
  if (!canApplyPartTime(item)) {
    ElMessage.warning('该兼职暂时无法申请')
    return
  }
  try {
    const response = await partTimeApi.apply(item.partTimeId, '')
    if (response.code === 200) {
      ElMessage.success('申请成功')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申请失败'))
  }
}

const createPartTime = async () => {
  const validationMessage = validateCreateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  isSubmitting.value = true
  try {
    const response = await partTimeApi.create(createForm)
    if (response.code === 200) {
      ElMessage.success('发布成功')
      showCreateModal.value = false
      resetCreateForm()
      await loadPartTimes()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发布失败'))
  } finally {
    isSubmitting.value = false
  }
}

const loadPartTimes = async () => {
  const response = isMerchant.value ? await partTimeApi.getMy() : await partTimeApi.getAll()
  if (response.code === 200) {
    partTimeList.value = response.data
  }
}

const closePartTime = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定下架「${item.title}」吗？下架后学生将无法继续申请该兼职。`,
      '下架招聘',
      {
        confirmButtonText: '下架',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    const response = await partTimeApi.close(item.partTimeId)
    if (response.code === 200) {
      ElMessage.success(response.message || '兼职已下架')
      await loadPartTimes()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '下架失败'))
    }
  }
}

const validateCreateForm = () => {
  if (!createForm.title) {
    return '请输入兼职标题'
  }
  if (!createForm.description) {
    return '请输入兼职描述'
  }
  if (Number(createForm.salaryMin) <= 0) {
    return '最低薪资必须大于 0'
  }
  if (Number(createForm.salaryMax) <= 0) {
    return '最高薪资必须大于 0'
  }
  if (Number(createForm.salaryMax) < Number(createForm.salaryMin)) {
    return '最高薪资不能小于最低薪资'
  }
  if (!Number.isInteger(Number(createForm.recruitCount)) || Number(createForm.recruitCount) <= 0) {
    return '请输入正确的招聘人数'
  }
  if (!createForm.workTime) {
    return '请输入工作时间'
  }
  return ''
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.description = ''
  createForm.salaryMin = 0
  createForm.salaryMax = 0
  createForm.recruitCount = 1
  createForm.workTime = ''
  createForm.location = ''
  createForm.requirements = ''
  createForm.images = []
}

const openContactDialog = (item) => {
  selectedItem.value = item
  showContactModal.value = true
}

const copyMerchantPhone = async () => {
  const phone = selectedItem.value?.merchant?.phone
  if (!phone) {
    ElMessage.warning('商家暂未设置手机号')
    return
  }

  try {
    await navigator.clipboard.writeText(phone)
    ElMessage.success('手机号已复制')
  } catch (error) {
    ElMessage.info(`商家手机号：${phone}`)
  }
}

const startChat = () => {
  const merchant = selectedItem.value?.merchant
  if (!merchant?.userId) {
    ElMessage.warning('商家信息不完整，暂时无法发起交流')
    return
  }
  router.push({
    path: '/messages',
    query: {
      peerId: merchant.userId,
      peerName: merchant.realName || merchant.username || '商家',
      targetType: 'part-time',
      targetId: selectedItem.value.partTimeId,
      targetTitle: selectedItem.value.title
    }
  })
}

onMounted(() => {
  userStore.initUser()
  loadPartTimes()
})
</script>

<style scoped>
.parttime-container {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header,
.search-bar,
.parttime-list {
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
  color: #168a52;
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
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 12px;
  margin-bottom: 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.parttime-list {
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
  background: #eff8f3;
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

.card-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 10px;
}

.card-top h3 {
  margin: 0;
  color: #172033;
  font-size: 19px;
  line-height: 1.35;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  min-height: 44px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
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

.salary {
  color: #67C23A;
  font-weight: bold;
  font-size: 18px;
}

.extra-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  color: #666;
  font-size: 14px;
}

.status {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  white-space: nowrap;
}

.status.pending {
  background: #fff3cd;
  color: #856404;
}

.status.active {
  background: #d1ecf1;
  color: #0c5460;
}

.status.closed {
  background: #f8d7da;
  color: #721c24;
}

.requirements {
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
  padding: 10px;
  line-height: 1.6;
  background: #f8fafc;
  border-radius: 8px;
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
  .parttime-container {
    padding: 16px;
  }

  .page-header,
  .search-bar {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
