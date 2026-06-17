<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <div class="profile-header">
        <div class="avatar">{{ getAvatarText(user?.realName || user?.username) }}</div>
        <div class="user-info">
          <div class="user-title">
            <h2>{{ user?.realName || '用户' }}</h2>
            <el-tag effect="plain">{{ getRoleText(user?.role) }}</el-tag>
          </div>
          <p>用户名：{{ user?.username || '未登录' }}</p>
          <p>手机号：{{ user?.phone || '未设置' }}</p>
          <p>邮箱：{{ user?.email || '未设置' }}</p>
          <p>注册时间：{{ formatTime(user?.createTime) }}</p>
        </div>
        <div class="header-actions">
          <el-button type="primary" plain @click="openEditDialog">编辑资料</el-button>
          <el-button :loading="loading" @click="loadData">刷新数据</el-button>
          <el-button type="danger" @click="logout">退出登录</el-button>
        </div>
      </div>

      <div class="stats">
        <div v-if="!isMerchant" class="stat-item">
          <div class="stat-value">{{ carpoolList.length }}</div>
          <div class="stat-label">我的拼车</div>
        </div>
        <div v-if="!isMerchant" class="stat-item">
          <div class="stat-value">{{ receivedApplications.length }}</div>
          <div class="stat-label">拼车申请</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ isMerchant ? receivedPartTimeApplications.length : partTimeList.length }}</div>
          <div class="stat-label">{{ isMerchant ? '兼职申请' : '我的兼职' }}</div>
        </div>
        <div v-if="!isMerchant" class="stat-item">
          <div class="stat-value">{{ secondhandList.length }}</div>
          <div class="stat-label">我的闲置</div>
        </div>
        <div v-if="!isMerchant" class="stat-item">
          <div class="stat-value">{{ lostFoundList.length }}</div>
          <div class="stat-label">失物招领</div>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="publish-tabs">
        <el-tab-pane v-if="isMerchant" label="兼职申请" name="part-time-applications">
          <el-empty v-if="!receivedPartTimeApplications.length" description="暂无收到的兼职申请" />
          <div v-else class="publish-list">
            <el-card v-for="item in receivedPartTimeApplications" :key="item.applyId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.title }}</h3>
                  <p>申请人：{{ item.applicant?.realName || item.applicant?.username }}</p>
                </div>
                <el-tag :type="getApplyStatusType(item.status)">{{ getApplyStatusText(item.status) }}</el-tag>
              </div>
              <div class="item-meta">
                <span>申请时间：{{ formatTime(item.createTime) }}</span>
                <span v-if="item.applicant?.phone">电话：{{ item.applicant.phone }}</span>
                <span>薪资：{{ formatMoney(item.salaryMin) }} - {{ formatMoney(item.salaryMax) }}/小时</span>
                <span>录用人数：{{ formatAcceptedCount(item) }}/{{ formatRecruitCount(item) }}人</span>
                <span>地点：{{ item.location || '未填写' }}</span>
              </div>
              <p v-if="item.resume" class="item-desc">申请说明：{{ item.resume }}</p>
              <div class="item-actions">
                <el-button
                  type="success"
                  plain
                  :disabled="item.status !== 'pending'"
                  :loading="handlingPartTimeApplyId === item.applyId"
                  @click="handlePartTimeApply(item, 'accepted')"
                >
                  同意
                </el-button>
                <el-button
                  type="danger"
                  plain
                  :disabled="item.status !== 'pending'"
                  :loading="handlingPartTimeApplyId === item.applyId"
                  @click="handlePartTimeApply(item, 'rejected')"
                >
                  拒绝
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="!isMerchant" label="我的拼车" name="carpool">
          <el-empty v-if="!carpoolList.length" description="暂无拼车发布记录" />
          <div v-else class="publish-list">
            <el-card v-for="item in carpoolList" :key="item.carpoolId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.departure }} → {{ item.destination }}</h3>
                  <p>{{ formatTime(item.departureTime) }}</p>
                </div>
                <el-tag :type="getCarpoolStatusType(item.status)">{{ getCarpoolStatusText(item.status) }}</el-tag>
              </div>
              <div class="item-meta">
                <span>座位：{{ item.seats }}</span>
                <span>价格：{{ formatMoney(item.price) }}</span>
              </div>
              <p v-if="item.description" class="item-desc">{{ item.description }}</p>
              <div class="item-actions">
                <el-button
                  type="danger"
                  plain
                  :disabled="item.status === 'canceled' || item.status === 'completed'"
                  :loading="cancelingCarpoolId === item.carpoolId"
                  @click="cancelCarpool(item.carpoolId)"
                >
                  取消拼车
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="!isMerchant" label="拼车申请" name="carpool-applications">
          <el-empty v-if="!receivedApplications.length" description="暂无收到的拼车申请" />
          <div v-else class="publish-list">
            <el-card v-for="item in receivedApplications" :key="item.applyId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.departure }} → {{ item.destination }}</h3>
                  <p>申请人：{{ item.applicant?.realName || item.applicant?.username }}</p>
                </div>
                <el-tag :type="getApplyStatusType(item.status)">{{ getApplyStatusText(item.status) }}</el-tag>
              </div>
              <div class="item-meta">
                <span>申请时间：{{ formatTime(item.createTime) }}</span>
                <span v-if="item.applicant?.phone">电话：{{ item.applicant.phone }}</span>
              </div>
              <div class="item-actions">
                <el-button
                  type="success"
                  plain
                  :disabled="item.status !== 'pending'"
                  :loading="handlingApplyId === item.applyId"
                  @click="handleCarpoolApply(item, 'accepted')"
                >
                  同意
                </el-button>
                <el-button
                  type="danger"
                  plain
                  :disabled="item.status !== 'pending'"
                  :loading="handlingApplyId === item.applyId"
                  @click="handleCarpoolApply(item, 'rejected')"
                >
                  拒绝
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="!isMerchant" label="我的兼职" name="part-time">
          <el-empty v-if="!partTimeList.length" description="暂无兼职发布记录" />
          <div v-else class="publish-list">
            <el-card v-for="item in partTimeList" :key="item.partTimeId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.workTime }}</p>
                </div>
                <el-tag :type="getPartTimeStatusType(item.status)">{{ getPartTimeStatusText(item.status) }}</el-tag>
              </div>
              <div class="item-meta">
                <span>薪资：{{ formatMoney(item.salaryMin) }} - {{ formatMoney(item.salaryMax) }}/小时</span>
                <span>地点：{{ item.location || '未填写' }}</span>
                <span>录用人数：{{ formatAcceptedCount(item) }}/{{ formatRecruitCount(item) }}人</span>
              </div>
              <p class="item-desc">{{ item.description }}</p>
              <div class="item-actions">
                <el-button
                  type="warning"
                  plain
                  :disabled="item.status === 'closed'"
                  :loading="closingPartTimeId === item.partTimeId"
                  @click="closePartTime(item.partTimeId)"
                >
                  关闭兼职
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="!isMerchant" label="我的闲置" name="secondhand">
          <el-empty v-if="!secondhandList.length" description="暂无闲置发布记录" />
          <div v-else class="publish-list">
            <el-card v-for="item in secondhandList" :key="item.secondhandId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.category }}</p>
                </div>
                <el-tag :type="getSecondhandStatusType(item.status)">{{ getSecondhandStatusText(item.status) }}</el-tag>
              </div>
              <div class="item-meta">
                <span>价格：{{ formatMoney(item.price) }}</span>
                <span>发布时间：{{ formatTime(item.createTime) }}</span>
              </div>
              <p v-if="item.description" class="item-desc">{{ item.description }}</p>
              <div class="item-actions">
                <el-button
                  type="success"
                  plain
                  :disabled="item.status === 'sold'"
                  :loading="sellingId === item.secondhandId"
                  @click="markItemAsSold(item.secondhandId)"
                >
                  标记售出
                </el-button>
                <el-button
                  type="danger"
                  plain
                  :disabled="item.status === 'offline'"
                  :loading="offliningSecondhandId === item.secondhandId"
                  @click="offlineSecondhand(item.secondhandId)"
                >
                  下架物品
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane v-if="!isMerchant" label="失物招领" name="lost-found">
          <el-empty v-if="!lostFoundList.length" description="暂无失物招领发布记录" />
          <div v-else class="publish-list">
            <el-card v-for="item in lostFoundList" :key="item.lostFoundId" class="publish-item" shadow="hover">
              <div class="item-top">
                <div>
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.location }}</p>
                </div>
                <div class="tag-stack">
                  <el-tag :type="item.type === 'lost' ? 'danger' : 'success'">{{ getLostFoundTypeText(item.type) }}</el-tag>
                  <el-tag :type="getLostFoundStatusType(item.status)">{{ getLostFoundStatusText(item.status) }}</el-tag>
                </div>
              </div>
              <div class="item-meta">
                <span>联系方式：{{ item.contact }}</span>
                <span>发布时间：{{ formatTime(item.createTime) }}</span>
              </div>
              <p class="item-desc">{{ item.description }}</p>
              <div class="item-actions">
                <el-button
                  type="success"
                  plain
                  :disabled="item.status === 'resolved'"
                  :loading="resolvingLostFoundId === item.lostFoundId"
                  @click="resolveLostFound(item.lostFoundId)"
                >
                  标记解决
                </el-button>
                <el-button
                  type="danger"
                  plain
                  :disabled="item.status === 'closed'"
                  :loading="closingLostFoundId === item.lostFoundId"
                  @click="closeLostFound(item.lostFoundId)"
                >
                  关闭信息
                </el-button>
              </div>
            </el-card>
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-dialog v-model="showEditDialog" title="编辑个人资料" width="460px">
        <el-form :model="editForm" label-width="90px">
          <el-form-item label="真实姓名">
            <el-input v-model.trim="editForm.realName" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model.trim="editForm.phone" maxlength="11" placeholder="可留空" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model.trim="editForm.email" maxlength="100" placeholder="可留空" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { carpoolApi, getErrorMessage, lostFoundApi, partTimeApi, secondhandApi, userApi } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const user = ref(null)
const loading = ref(false)
const sellingId = ref(null)
const cancelingCarpoolId = ref(null)
const closingPartTimeId = ref(null)
const offliningSecondhandId = ref(null)
const handlingApplyId = ref(null)
const handlingPartTimeApplyId = ref(null)
const resolvingLostFoundId = ref(null)
const closingLostFoundId = ref(null)
const activeTab = ref('carpool')
const showEditDialog = ref(false)
const savingProfile = ref(false)
const carpoolList = ref([])
const receivedApplications = ref([])
const partTimeList = ref([])
const receivedPartTimeApplications = ref([])
const secondhandList = ref([])
const lostFoundList = ref([])
const editForm = ref({
  realName: '',
  phone: '',
  email: ''
})

const getRoleText = (role) => {
  const map = {
    student: '学生',
    merchant: '商家',
    admin: '管理员'
  }
  return map[role] || role || '未设置'
}

const isMerchant = computed(() => user.value?.role === 'merchant')

const getAvatarText = (name) => {
  return name ? String(name).trim().slice(0, 1).toUpperCase() : 'U'
}

const getCarpoolStatusText = (status) => ({
  pending: '待审核',
  active: '招募中',
  confirmed: '已确认',
  completed: '已完成',
  canceled: '已取消'
}[status] || status)

const getCarpoolStatusType = (status) => ({
  pending: 'warning',
  active: 'success',
  confirmed: 'success',
  completed: 'info',
  canceled: 'danger'
}[status] || 'info')

const getApplyStatusText = (status) => ({
  pending: '待处理',
  accepted: '已同意',
  rejected: '已拒绝'
}[status] || status)

const getApplyStatusType = (status) => ({
  pending: 'warning',
  accepted: 'success',
  rejected: 'danger'
}[status] || 'info')

const getPartTimeStatusText = (status) => ({
  pending: '待审核',
  active: '招聘中',
  closed: '已关闭'
}[status] || status)

const getPartTimeStatusType = (status) => ({
  pending: 'warning',
  active: 'success',
  closed: 'info'
}[status] || 'info')

const getSecondhandStatusText = (status) => ({
  pending: '待审核',
  active: '在售',
  sold: '已售出',
  offline: '已下架'
}[status] || status)

const getSecondhandStatusType = (status) => ({
  pending: 'warning',
  active: 'success',
  sold: 'info',
  offline: 'danger'
}[status] || 'info')

const getLostFoundTypeText = (type) => ({
  lost: '寻物',
  found: '招领'
}[type] || type)

const getLostFoundStatusText = (status) => ({
  open: '进行中',
  resolved: '已解决',
  closed: '已关闭'
}[status] || status)

const getLostFoundStatusType = (status) => ({
  open: 'primary',
  resolved: 'success',
  closed: 'info'
}[status] || 'info')

const formatTime = (time) => {
  if (!time) return '未知'
  return new Date(time).toLocaleString('zh-CN')
}

const formatMoney = (value) => {
  if (value === null || value === undefined || value === '') return '¥0.00'
  return `¥${Number(value).toFixed(2)}`
}

const formatRecruitCount = (item) => {
  const count = Number(item?.recruitCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 1
}

const formatAcceptedCount = (item) => {
  const count = Number(item?.acceptedCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}

const syncEditForm = () => {
  editForm.value = {
    realName: user.value?.realName || '',
    phone: user.value?.phone || '',
    email: user.value?.email || ''
  }
}

const openEditDialog = () => {
  syncEditForm()
  showEditDialog.value = true
}

const loadData = async () => {
  loading.value = true
  try {
    const profileRes = await userStore.getProfile()
    user.value = profileRes.code === 200 ? profileRes.data : userStore.user

    if (isMerchant.value) {
      const applicationRes = await partTimeApi.getReceivedApplications()
      if (applicationRes.code === 200) receivedPartTimeApplications.value = applicationRes.data
      carpoolList.value = []
      receivedApplications.value = []
      partTimeList.value = []
      secondhandList.value = []
      lostFoundList.value = []
      activeTab.value = 'part-time-applications'
    } else {
      const [carpoolRes, applicationRes, partTimeRes, secondhandRes, lostFoundRes] = await Promise.all([
        carpoolApi.getMy(),
        carpoolApi.getReceivedApplications(),
        partTimeApi.getMy(),
        secondhandApi.getMy(),
        lostFoundApi.getMy()
      ])

      if (carpoolRes.code === 200) carpoolList.value = carpoolRes.data
      if (applicationRes.code === 200) receivedApplications.value = applicationRes.data
      if (partTimeRes.code === 200) partTimeList.value = partTimeRes.data
      if (secondhandRes.code === 200) secondhandList.value = secondhandRes.data
      if (lostFoundRes.code === 200) lostFoundList.value = lostFoundRes.data
      receivedPartTimeApplications.value = []
    }

    syncEditForm()
  } catch (error) {
    user.value = userStore.user
    ElMessage.error(getErrorMessage(error, '加载个人中心数据失败'))
  } finally {
    loading.value = false
  }
}

const saveProfile = async () => {
  if (!editForm.value.realName) {
    ElMessage.warning('请输入真实姓名')
    return
  }

  savingProfile.value = true
  try {
    const updateResponse = await userApi.updateProfile(editForm.value)
    if (updateResponse.code === 200) {
      user.value = updateResponse.data
      userStore.user = updateResponse.data
      localStorage.setItem('user', JSON.stringify(updateResponse.data))
      showEditDialog.value = false
      ElMessage.success(updateResponse.message || '资料更新成功')
    } else {
      ElMessage.error(updateResponse.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '资料更新失败'))
  } finally {
    savingProfile.value = false
  }
}

const handleCarpoolApply = async (item, status) => {
  handlingApplyId.value = item.applyId
  try {
    const response = await carpoolApi.handleApply(item.carpoolId, item.applyId, status)
    if (response.code === 200) {
      ElMessage.success(status === 'accepted' ? '已同意拼车申请' : '已拒绝拼车申请')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '处理申请失败'))
  } finally {
    handlingApplyId.value = null
  }
}

const handlePartTimeApply = async (item, status) => {
  handlingPartTimeApplyId.value = item.applyId
  try {
    const response = await partTimeApi.handleApply(item.partTimeId, item.applyId, status)
    if (response.code === 200) {
      ElMessage.success(status === 'accepted' ? '已同意兼职申请' : '已拒绝兼职申请')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '处理兼职申请失败'))
  } finally {
    handlingPartTimeApplyId.value = null
  }
}

const cancelCarpool = async (id) => {
  cancelingCarpoolId.value = id
  try {
    const response = await carpoolApi.cancel(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '拼车已取消')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '取消拼车失败'))
  } finally {
    cancelingCarpoolId.value = null
  }
}

const closePartTime = async (id) => {
  closingPartTimeId.value = id
  try {
    const response = await partTimeApi.close(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '兼职已关闭')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '关闭兼职失败'))
  } finally {
    closingPartTimeId.value = null
  }
}

const markItemAsSold = async (id) => {
  sellingId.value = id
  try {
    const response = await secondhandApi.markAsSold(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '已标记为售出')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '标记售出失败'))
  } finally {
    sellingId.value = null
  }
}

const offlineSecondhand = async (id) => {
  offliningSecondhandId.value = id
  try {
    const response = await secondhandApi.offline(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '物品已下架')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '下架物品失败'))
  } finally {
    offliningSecondhandId.value = null
  }
}

const resolveLostFound = async (id) => {
  resolvingLostFoundId.value = id
  try {
    const response = await lostFoundApi.resolve(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '已标记为解决')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '标记解决失败'))
  } finally {
    resolvingLostFoundId.value = null
  }
}

const closeLostFound = async (id) => {
  closingLostFoundId.value = id
  try {
    const response = await lostFoundApi.close(id)
    if (response.code === 200) {
      ElMessage.success(response.message || '信息已关闭')
      await loadData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '关闭信息失败'))
  } finally {
    closingLostFoundId.value = null
  }
}

onMounted(() => {
  userStore.initUser()
  user.value = userStore.user
  activeTab.value = isMerchant.value ? 'part-time-applications' : 'carpool'
  syncEditForm()
  loadData()
})

watch(isMerchant, (value) => {
  activeTab.value = value ? 'part-time-applications' : 'carpool'
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 1120px;
  margin: 0 auto;
}

.profile-card {
  padding: 24px;
}

.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid #eee;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border-radius: 50%;
}

.user-info {
  flex: 1;
}

.user-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.user-info h2 {
  margin: 0;
}

.user-info p {
  margin: 8px 0;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  padding: 20px;
  text-align: center;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-value {
  margin-bottom: 8px;
  color: #303133;
  font-size: 28px;
  font-weight: 700;
}

.stat-label {
  color: #666;
}

.publish-tabs {
  margin-top: 8px;
}

.publish-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 16px;
}

.publish-item {
  border-radius: 8px;
}

.item-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.item-top h3 {
  margin: 0 0 8px;
  color: #303133;
}

.item-top p,
.item-meta {
  color: #606266;
  font-size: 14px;
}

.item-top p {
  margin: 0;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.item-desc {
  margin: 0;
  color: #666;
  line-height: 1.6;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.tag-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

@media (max-width: 900px) {
  .profile-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
  }

  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .stats {
    grid-template-columns: 1fr;
  }
}
</style>
