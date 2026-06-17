<template>
  <main class="secondhand-container">
    <section class="page-header">
      <div>
        <span>二手好物</span>
        <h1>闲置交易</h1>
      </div>
      <el-button type="primary" @click="showCreateModal = true">发布闲置</el-button>
    </section>

    <section class="filter-bar">
      <el-select v-model="filterForm.category" placeholder="分类">
        <el-option label="全部" value="" />
        <el-option label="电子产品" value="电子产品" />
        <el-option label="书籍教材" value="书籍教材" />
        <el-option label="服饰鞋包" value="服饰鞋包" />
        <el-option label="生活用品" value="生活用品" />
        <el-option label="其他" value="其他" />
      </el-select>
      <el-input v-model.trim="filterForm.keyword" clearable placeholder="搜索物品、描述或分类" />
      <el-button type="primary" @click="filter">筛选</el-button>
    </section>

    <section class="secondhand-list">
      <el-empty v-if="!secondhandList.length" description="暂无闲置信息" />
      <el-card v-for="item in secondhandList" :key="item.secondhandId" class="item-card">
        <div class="item-image">
          <img v-if="item.images && item.images.length > 0" :src="item.images[0]" alt="物品图片" />
          <div v-else class="no-image">
            <el-icon><Picture /></el-icon>
          </div>
        </div>
        <h3>{{ item.title }}</h3>
        <p class="description">{{ item.description }}</p>
        <div class="info">
          <span class="category">{{ item.category }}</span>
          <span class="price">¥{{ item.price }}</span>
        </div>
        <div class="extra">
          <span>发布者：{{ item.user?.realName || item.user?.username }}</span>
          <span class="status" :class="item.status">{{ getStatusText(item.status) }}</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" plain @click="openContactDialog(item)">联系卖家</el-button>
          <el-button
            type="success"
            :disabled="!canCreateOrder(item)"
            :loading="orderingId === item.secondhandId"
            @click="createOrder(item)"
          >
            立即下单
          </el-button>
        </div>
      </el-card>
    </section>

    <el-dialog v-model="showContactModal" title="联系卖家" width="420px">
      <div v-if="selectedItem" class="contact-panel">
        <div class="contact-goods">
          <strong>{{ selectedItem.title }}</strong>
          <span>{{ selectedItem.category }} · ¥{{ selectedItem.price }}</span>
        </div>
        <div class="contact-row">
          <span>卖家</span>
          <strong>{{ getSellerName(selectedItem) }}</strong>
        </div>
        <div class="contact-row">
          <span>用户名</span>
          <strong>{{ selectedItem.user?.username || '未提供' }}</strong>
        </div>
        <div class="contact-row">
          <span>手机号</span>
          <strong>{{ selectedItem.user?.phone || '卖家暂未设置手机号' }}</strong>
        </div>
        <el-alert
          v-if="!selectedItem.user?.phone"
          title="卖家还没有在个人中心填写手机号，可以先通过用户名联系或提醒卖家完善资料。"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
      <template #footer>
        <el-button @click="showContactModal = false">关闭</el-button>
        <el-button type="success" @click="startChat">在线交流</el-button>
        <el-button
          type="primary"
          :disabled="!selectedItem?.user?.phone"
          @click="copySellerPhone"
        >
          复制手机号
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateModal" title="发布闲置物品">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model.trim="createForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model.trim="createForm.description"
            type="textarea"
            :rows="4"
            maxlength="300"
            show-word-limit
            placeholder="可填写新旧程度、交易地点和配件情况"
          />
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model.number="createForm.price" type="number" min="0.01" step="0.01" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.category">
            <el-option label="电子产品" value="电子产品" />
            <el-option label="书籍教材" value="书籍教材" />
            <el-option label="服饰鞋包" value="服饰鞋包" />
            <el-option label="生活用品" value="生活用品" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <ImageUploader v-model="createForm.images" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateModal = false">取消</el-button>
        <el-button type="primary" :loading="isSubmitting" @click="createSecondhand">发布</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getErrorMessage, secondhandApi } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import ImageUploader from '../components/ImageUploader.vue'
import { orderApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const secondhandList = ref([])
const showCreateModal = ref(false)
const showContactModal = ref(false)
const selectedItem = ref(null)
const isSubmitting = ref(false)
const orderingId = ref(null)

const filterForm = reactive({
  category: '',
  keyword: ''
})

const createForm = reactive({
  title: '',
  description: '',
  price: 0,
  category: '',
  images: []
})

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    active: '在售',
    sold: '已售出',
    offline: '已下架'
  }
  return map[status] || status
}

const filter = async () => {
  let response
  if (filterForm.category) {
    response = await secondhandApi.getByCategory(filterForm.category)
  } else {
    response = await secondhandApi.getAll()
  }
  
  if (response.code === 200) {
    let data = response.data
    if (filterForm.keyword) {
      const keyword = filterForm.keyword.trim()
      data = data.filter(item => (item.title || '').includes(keyword) ||
                                 (item.description || '').includes(keyword) ||
                                 (item.category || '').includes(keyword))
    }
    secondhandList.value = data
  }
}

const getSellerName = (item) => {
  return item?.user?.realName || item?.user?.username || '未提供'
}

const canCreateOrder = (item) => {
  return item.status !== 'sold' &&
    item.status !== 'offline' &&
    item.user?.userId !== userStore.user?.userId
}

const openContactDialog = (item) => {
  selectedItem.value = item
  showContactModal.value = true
}

const copySellerPhone = async () => {
  const phone = selectedItem.value?.user?.phone
  if (!phone) {
    ElMessage.warning('卖家暂未设置手机号')
    return
  }

  try {
    await navigator.clipboard.writeText(phone)
    ElMessage.success('手机号已复制')
  } catch (error) {
    ElMessage.info(`卖家手机号：${phone}`)
  }
}

const startChat = () => {
  const seller = selectedItem.value?.user
  if (!seller?.userId) {
    ElMessage.warning('卖家信息不完整，暂时无法发起交流')
    return
  }

  router.push({
    path: '/messages',
    query: {
      peerId: seller.userId,
      peerName: seller.realName || seller.username || '卖家',
      targetType: 'secondhand',
      targetId: selectedItem.value.secondhandId,
      targetTitle: selectedItem.value.title
    }
  })
}

const createOrder = async (item) => {
  if (item.user?.userId === userStore.user?.userId) {
    ElMessage.warning('不能购买自己发布的物品')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定购买「${item.title}」吗？订单金额 ${formatMoney(item.price)}。`,
      '确认下单',
      {
        confirmButtonText: '下单',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    orderingId.value = item.secondhandId
    const response = await orderApi.create({
      type: 'secondhand',
      targetId: item.secondhandId,
      amount: item.price
    })
    if (response.code === 200) {
      ElMessage.success('订单创建成功，请前往我的订单支付')
      router.push('/orders')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '下单失败'))
    }
  } finally {
    orderingId.value = null
  }
}

const formatMoney = (value) => {
  const amount = Number(value)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const createSecondhand = async () => {
  const validationMessage = validateCreateForm()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }

  isSubmitting.value = true
  try {
    const response = await secondhandApi.create(createForm)
    if (response.code === 200) {
      ElMessage.success('发布成功')
      showCreateModal.value = false
      resetCreateForm()
      await loadSecondhands()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '发布失败'))
  } finally {
    isSubmitting.value = false
  }
}

const loadSecondhands = async () => {
  const response = await secondhandApi.getAll()
  if (response.code === 200) {
    secondhandList.value = response.data
  }
}

const validateCreateForm = () => {
  if (!createForm.title) {
    return '请输入物品标题'
  }
  if (Number(createForm.price) <= 0) {
    return '价格必须大于 0'
  }
  if (!createForm.category) {
    return '请选择物品分类'
  }
  return ''
}

const resetCreateForm = () => {
  createForm.title = ''
  createForm.description = ''
  createForm.price = 0
  createForm.category = ''
  createForm.images = []
}

onMounted(loadSecondhands)
</script>

<style scoped>
.secondhand-container {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header,
.filter-bar,
.secondhand-list {
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
  color: #b85d00;
  font-size: 13px;
  font-weight: 700;
}

.page-header h1 {
  margin: 6px 0 0;
  color: #172033;
  font-size: 28px;
  line-height: 1.25;
}

.filter-bar {
  display: grid;
  grid-template-columns: 180px minmax(220px, 1fr) auto;
  gap: 12px;
  margin-bottom: 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.secondhand-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  min-height: 220px;
}

.item-card {
  border-radius: 8px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.item-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 14px 28px rgba(31, 45, 61, 0.1);
}

.item-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  margin-bottom: 15px;
  border-radius: 8px;
  background: #f1f5f9;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #f5f5f5;
  color: #93a4b7;
}

.no-image .el-icon {
  font-size: 48px;
}

.item-card h3 {
  margin: 0 0 10px;
  color: #172033;
  font-size: 18px;
  line-height: 1.35;
}

.description {
  color: #666;
  font-size: 14px;
  margin-bottom: 10px;
  min-height: 42px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.category {
  background: #f0f9ff;
  color: #03a9f4;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 18px;
}

.extra {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 15px;
  color: #666;
  font-size: 14px;
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

.status.active {
  background: #d4edda;
  color: #155724;
}

.status.sold {
  background: #d1ecf1;
  color: #0c5460;
}

.status.offline {
  background: #fde2e2;
  color: #b42318;
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

.card-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.card-actions :deep(.el-button) {
  margin: 0;
}

@media (max-width: 640px) {
  .secondhand-container {
    padding: 16px;
  }

  .page-header,
  .filter-bar {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
