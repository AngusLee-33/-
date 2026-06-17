<template>
  <main class="home-container" :class="{ 'merchant-home': isMerchant }">
    <section class="hero">
      <div class="hero-copy">
        <el-tag effect="dark" class="hero-tag">{{ isMerchant ? '商家工作台' : '校园生活一站式' }}</el-tag>
        <h1>{{ isMerchant ? '兼职招聘管理' : '校园综合服务平台' }}</h1>
        <p>{{ isMerchant ? '发布兼职岗位、处理学生申请，并通过消息功能与申请人保持沟通。' : '把拼车、兼职、闲置交易和失物招领集中到一个入口，常用事项进来就能处理。' }}</p>
      </div>

      <div class="hero-board">
        <div class="board-header">
          <span>{{ isMerchant ? '商家服务概览' : '今日服务概览' }}</span>
          <strong>{{ todayText }}</strong>
        </div>
        <div class="overview-panel">
          <div v-for="item in overview" :key="item.label" class="metric">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
        <div class="board-tip">
          <el-icon><Clock /></el-icon>
          <span>{{ isMerchant ? '建议优先处理待回复的兼职申请，及时完成招聘确认。' : '建议优先查看最新拼车和兼职申请，避免错过时间。' }}</span>
        </div>
      </div>
    </section>

    <section class="service-grid">
      <button
        v-for="service in services"
        :key="service.path"
        class="service-card"
        type="button"
        @click="navigate(service.path)"
      >
        <span class="service-icon" :class="service.tone">
          <el-icon><component :is="service.icon" /></el-icon>
        </span>
        <span class="service-body">
          <strong>{{ service.title }}</strong>
          <small>{{ service.desc }}</small>
        </span>
        <el-icon class="service-arrow"><ArrowRight /></el-icon>
      </button>
    </section>

    <section v-if="isMerchant" class="merchant-dashboard">
      <div class="merchant-panel applications-panel">
        <div class="section-heading">
          <div>
            <span>待处理申请</span>
            <h2>学生兼职申请</h2>
          </div>
          <el-button link type="primary" @click="navigate('/profile')">处理全部</el-button>
        </div>

        <el-empty v-if="!pendingApplications.length" description="暂无待处理申请" />
        <article
          v-for="item in pendingApplications"
          v-else
          :key="item.applyId"
          class="application-item"
        >
          <div class="applicant-avatar">{{ getAvatarText(item.applicant?.realName || item.applicant?.username) }}</div>
          <div class="application-main">
            <h3>{{ item.applicant?.realName || item.applicant?.username || '申请人' }}</h3>
            <p>{{ item.title }}</p>
            <small>{{ formatTime(item.createTime) }}</small>
          </div>
          <el-tag type="warning" effect="plain">待处理</el-tag>
        </article>
      </div>

      <div class="merchant-panel jobs-panel">
        <div class="section-heading">
          <div>
            <span>岗位状态</span>
            <h2>我的招聘信息</h2>
          </div>
          <el-button link type="primary" @click="navigate('/part-time')">发布/管理</el-button>
        </div>

        <el-empty v-if="!partTimeList.length" description="暂无已发布兼职" />
        <article v-for="item in partTimeList" v-else :key="item.partTimeId" class="job-row">
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.workTime }} · {{ item.location || '地点待定' }}</p>
          </div>
          <div class="job-progress">
            <span>{{ formatAcceptedCount(item) }}/{{ formatRecruitCount(item) }}人</span>
            <el-progress
              :percentage="getRecruitProgress(item)"
              :stroke-width="8"
              :show-text="false"
              :status="isJobFull(item) ? 'success' : undefined"
            />
          </div>
          <div class="job-actions">
            <el-tag :type="getPartTimeStatusType(item)" effect="plain">{{ getPartTimeStatusText(item) }}</el-tag>
            <el-button
              v-if="item.status !== 'closed'"
              size="small"
              type="danger"
              plain
              @click="closePartTime(item)"
            >
              下架
            </el-button>
          </div>
        </article>
      </div>
    </section>

    <section v-else class="content-grid">
      <div v-if="!isMerchant" class="section-panel">
        <div class="section-heading">
          <div>
            <span>热门拼车</span>
            <h2>近期出行</h2>
          </div>
          <el-button link type="primary" @click="navigate('/carpool')">查看全部</el-button>
        </div>

        <el-empty v-if="!carpoolList.length" description="暂无拼车信息" />
        <article v-for="item in carpoolList" v-else :key="item.carpoolId" class="list-item">
          <div class="item-main">
            <h3>{{ item.departure }} → {{ item.destination }}</h3>
            <p>
              <el-icon><Clock /></el-icon>
              {{ formatTime(item.departureTime) }}
            </p>
          </div>
          <div class="item-side">
            <el-tag size="small" :type="getStatusType(item.status)">{{ getStatusText(item.status) }}</el-tag>
            <strong>{{ formatMoney(item.price) }}</strong>
            <small>剩余 {{ item.seats }} 座</small>
          </div>
          <el-button type="primary" plain @click="applyCarpool(item.carpoolId)">申请</el-button>
        </article>
      </div>

      <div class="section-panel">
        <div class="section-heading">
          <div>
            <span>最新兼职</span>
            <h2>校内机会</h2>
          </div>
          <el-button link type="primary" @click="navigate('/part-time')">查看全部</el-button>
        </div>

        <el-empty v-if="!partTimeList.length" description="暂无兼职信息" />
        <article v-for="item in partTimeList" v-else :key="item.partTimeId" class="list-item">
          <div class="item-main">
            <h3>{{ item.title }}</h3>
            <p>{{ item.workTime }}</p>
          </div>
          <div class="item-side">
            <strong>{{ formatMoney(item.salaryMin) }}-{{ formatMoney(item.salaryMax) }}/小时</strong>
            <small>{{ item.location || '地点待定' }}</small>
            <small>已录用 {{ formatAcceptedCount(item) }}/{{ formatRecruitCount(item) }} 人</small>
          </div>
          <el-button type="primary" plain @click="applyPartTime(item.partTimeId)">申请</el-button>
        </article>
      </div>

      <div v-if="!isMerchant" class="section-panel">
        <div class="section-heading">
          <div>
            <span>精选闲置</span>
            <h2>闲置好物</h2>
          </div>
          <el-button link type="primary" @click="navigate('/secondhand')">查看全部</el-button>
        </div>

        <el-empty v-if="!secondhandList.length" description="暂无闲置信息" />
        <div v-else class="goods-grid">
          <article v-for="item in secondhandList" :key="item.secondhandId" class="goods-item">
            <div class="goods-cover">
              <img v-if="getCover(item)" :src="getCover(item)" :alt="item.title" />
              <el-icon v-else><Box /></el-icon>
            </div>
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.category }}</p>
            </div>
            <strong>{{ formatMoney(item.price) }}</strong>
          </article>
        </div>
      </div>

      <div v-if="!isMerchant" class="section-panel">
        <div class="section-heading">
          <div>
            <span>失物招领</span>
            <h2>最新线索</h2>
          </div>
          <el-button link type="primary" @click="navigate('/lost-found')">查看全部</el-button>
        </div>

        <el-empty v-if="!lostFoundList.length" description="暂无失物招领" />
        <article v-for="item in lostFoundList" v-else :key="item.lostFoundId" class="notice-item">
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.location || '地点待补充' }}</p>
          </div>
          <el-tag size="small" :type="item.type === 'lost' ? 'danger' : 'success'">
            {{ item.type === 'lost' ? '寻物' : '招领' }}
          </el-tag>
        </article>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowRight,
  Box,
  Briefcase,
  Clock,
  Goods,
  ChatDotRound,
  Search,
  User,
  Van
} from '@element-plus/icons-vue'
import { carpoolApi, getErrorMessage, lostFoundApi, partTimeApi, secondhandApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const carpoolList = ref([])
const partTimeList = ref([])
const partTimeApplications = ref([])
const secondhandList = ref([])
const lostFoundList = ref([])
const todayText = new Date().toLocaleDateString('zh-CN', {
  month: '2-digit',
  day: '2-digit',
  weekday: 'long'
})

const isMerchant = computed(() => userStore.user?.role === 'merchant')

const studentServices = [
  {
    title: '拼车服务',
    desc: '同路同行，节省出行成本',
    path: '/carpool',
    icon: Van,
    tone: 'blue'
  },
  {
    title: '兼职招聘',
    desc: '查看校内外灵活岗位',
    path: '/part-time',
    icon: Briefcase,
    tone: 'green'
  },
  {
    title: '闲置交易',
    desc: '发布和浏览二手好物',
    path: '/secondhand',
    icon: Goods,
    tone: 'orange'
  },
  {
    title: '失物招领',
    desc: '找回校园卡、钥匙和随身物品',
    path: '/lost-found',
    icon: Search,
    tone: 'teal'
  }
]

const merchantServices = [
  {
    title: '兼职招聘',
    desc: '发布岗位，管理招聘信息',
    path: '/part-time',
    icon: Briefcase,
    tone: 'green'
  },
  {
    title: '消息交流',
    desc: '与学生进行一对一沟通',
    path: '/messages',
    icon: ChatDotRound,
    tone: 'blue'
  },
  {
    title: '个人中心',
    desc: '处理收到的兼职申请',
    path: '/profile',
    icon: User,
    tone: 'teal'
  }
]

const services = computed(() => isMerchant.value ? merchantServices : studentServices)

const overview = computed(() => {
  if (isMerchant.value) {
    const activeJobs = partTimeList.value.filter(item => item.status === 'active').length
    const pendingJobs = partTimeList.value.filter(item => item.status === 'pending').length
    const fullJobs = partTimeList.value.filter(item => formatAcceptedCount(item) >= formatRecruitCount(item)).length
    return [
      { label: '待处理申请', value: pendingApplications.value.length },
      { label: '招募中', value: activeJobs },
      { label: '待审核', value: pendingJobs },
      { label: '已招满', value: fullJobs }
    ]
  }
  return [
    { label: '拼车信息', value: carpoolList.value.length },
    { label: '兼职岗位', value: partTimeList.value.length },
    { label: '闲置好物', value: secondhandList.value.length },
    { label: '失物招领', value: lostFoundList.value.length }
  ]
})

const pendingApplications = computed(() => {
  return partTimeApplications.value
    .filter(item => item.status === 'pending')
    .slice(0, 5)
})

const navigate = (path) => {
  router.push(path)
}

const getStatusText = (status) => {
  const map = {
    pending: '待审核',
    confirmed: '已确认',
    completed: '已完成',
    canceled: '已取消',
    active: '进行中'
  }
  return map[status] || status || '未知'
}

const getStatusType = (status) => {
  const map = {
    pending: 'warning',
    confirmed: 'success',
    completed: 'info',
    canceled: 'danger',
    active: 'success'
  }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return '时间待定'
  return new Date(time).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatMoney = (value) => {
  const amount = Number(value)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const formatRecruitCount = (item) => {
  const count = Number(item?.recruitCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 1
}

const formatAcceptedCount = (item) => {
  const count = Number(item?.acceptedCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

const getRecruitProgress = (item) => {
  const recruitCount = formatRecruitCount(item)
  if (!recruitCount) return 0
  return Math.min(100, Math.round((formatAcceptedCount(item) / recruitCount) * 100))
}

const isJobFull = (item) => {
  return formatAcceptedCount(item) >= formatRecruitCount(item)
}

const getPartTimeStatusText = (item) => {
  if (isJobFull(item)) return '已招满'
  const map = {
    pending: '待审核',
    active: '招募中',
    closed: '已关闭'
  }
  return map[item?.status] || item?.status || '未知'
}

const getPartTimeStatusType = (item) => {
  if (isJobFull(item)) return 'success'
  const map = {
    pending: 'warning',
    active: 'primary',
    closed: 'info'
  }
  return map[item?.status] || 'info'
}

const getAvatarText = (name) => {
  return name ? String(name).trim().slice(0, 1).toUpperCase() : '申'
}

const getCover = (item) => {
  if (Array.isArray(item.images)) return item.images[0]
  return item.images || ''
}

const applyCarpool = async (id) => {
  try {
    const response = await carpoolApi.apply(id)
    response.code === 200 ? ElMessage.success('申请成功') : ElMessage.error(response.message)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申请失败'))
  }
}

const applyPartTime = async (id) => {
  try {
    const response = await partTimeApi.apply(id, '')
    response.code === 200 ? ElMessage.success('申请成功') : ElMessage.error(response.message)
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '申请失败'))
  }
}

const loadMerchantData = async () => {
  const [partTimeRes, applicationRes] = await Promise.all([
    partTimeApi.getMy(),
    partTimeApi.getReceivedApplications()
  ])
  if (partTimeRes.code === 200) partTimeList.value = partTimeRes.data.slice(0, 4)
  if (applicationRes.code === 200) partTimeApplications.value = applicationRes.data || []
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
      await loadMerchantData()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '下架失败'))
    }
  }
}

onMounted(async () => {
  userStore.initUser()
  try {
    if (isMerchant.value) {
      await loadMerchantData()
      return
    }

    const [carpoolRes, partTimeRes, secondhandRes, lostFoundRes] = await Promise.all([
      carpoolApi.getAll(),
      partTimeApi.getAll(),
      secondhandApi.getAll(),
      lostFoundApi.getAll()
    ])
    if (carpoolRes.code === 200) carpoolList.value = carpoolRes.data.slice(0, 4)
    if (partTimeRes.code === 200) partTimeList.value = partTimeRes.data.slice(0, 4)
    if (secondhandRes.code === 200) secondhandList.value = secondhandRes.data.slice(0, 6)
    if (lostFoundRes.code === 200) lostFoundList.value = lostFoundRes.data.slice(0, 4)
  } catch (error) {
    console.error('加载首页数据失败', error)
  }
})
</script>

<style scoped>
.home-container {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background:
    linear-gradient(135deg, rgba(31, 94, 255, 0.1) 0%, rgba(255, 255, 255, 0) 38%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.7fr);
  gap: 24px;
  max-width: 1240px;
  margin: 0 auto 24px;
  overflow: hidden;
  color: #ffffff;
  background:
    linear-gradient(90deg, rgba(23, 34, 53, 0.88) 0%, rgba(23, 34, 53, 0.58) 100%),
    url("https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1400&q=80") center/cover;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(31, 45, 61, 0.08);
}

.hero-copy,
.hero-board {
  position: relative;
  z-index: 1;
}

.hero-copy {
  padding: 46px 0 46px 44px;
}

.hero-tag {
  width: fit-content;
  border: 0;
  background: #1f5eff;
}

.hero h1 {
  margin: 18px 0 12px;
  color: #ffffff;
  font-size: 44px;
  line-height: 1.12;
  letter-spacing: 0;
}

.hero p {
  max-width: 620px;
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 17px;
  line-height: 1.8;
}

.hero-board {
  align-self: stretch;
  margin: 28px 28px 28px 0;
  padding: 22px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  box-shadow: 0 18px 38px rgba(13, 24, 41, 0.18);
}

.board-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  color: #172033;
}

.board-header span {
  font-size: 14px;
  font-weight: 700;
}

.board-header strong {
  color: #64748b;
  font-size: 13px;
}

.overview-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.metric {
  display: grid;
  gap: 8px;
  min-height: 92px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid #e8edf4;
  border-radius: 8px;
}

.metric span {
  color: #64748b;
  font-size: 14px;
}

.metric strong {
  color: #172033;
  font-size: 30px;
  line-height: 1;
}

.board-tip {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-top: 16px;
  padding: 12px;
  color: #476176;
  background: #eef6ff;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.service-grid,
.content-grid {
  max-width: 1240px;
  margin: 0 auto;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.merchant-home .service-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.service-card {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 20px;
  gap: 14px;
  align-items: center;
  min-height: 96px;
  padding: 18px;
  text-align: left;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.service-card:hover {
  transform: translateY(-3px);
  border-color: #b8cdf7;
  box-shadow: 0 14px 30px rgba(31, 45, 61, 0.1);
}

.service-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  font-size: 24px;
}

.service-icon.blue {
  color: #1f5eff;
  background: #eaf1ff;
}

.service-icon.green {
  color: #168a52;
  background: #e9f8f1;
}

.service-icon.orange {
  color: #b85d00;
  background: #fff3e2;
}

.service-icon.teal {
  color: #04756f;
  background: #e7f7f5;
}

.service-icon.purple {
  color: #7354c9;
  background: #f0edff;
}

.service-body {
  display: grid;
  gap: 6px;
}

.service-body strong {
  color: #172033;
  font-size: 16px;
}

.service-body small,
.service-arrow {
  color: #748294;
}

.section-heading span {
  color: #1f5eff;
  font-size: 13px;
  font-weight: 700;
}

.merchant-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
  max-width: 1240px;
  margin: 0 auto;
}

.merchant-panel {
  min-height: 390px;
  padding: 22px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
  box-shadow: 0 14px 30px rgba(31, 45, 61, 0.06);
}

.application-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 14px 0;
  border-top: 1px solid #edf1f6;
}

.application-item:first-of-type {
  border-top: 0;
}

.applicant-avatar {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  color: #ffffff;
  font-weight: 700;
  background: #1f5eff;
  border-radius: 8px;
}

.application-main h3,
.job-row h3 {
  margin: 0 0 6px;
  color: #172033;
  font-size: 16px;
}

.application-main p,
.job-row p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.application-main small {
  display: block;
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.job-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px auto;
  gap: 16px;
  align-items: center;
  padding: 15px 0;
  border-top: 1px solid #edf1f6;
}

.job-row:first-of-type {
  border-top: 0;
}

.job-progress {
  display: grid;
  gap: 8px;
}

.job-progress span {
  color: #64748b;
  font-size: 13px;
  text-align: right;
}

.job-actions {
  display: inline-flex;
  justify-content: flex-end;
  gap: 8px;
  align-items: center;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.section-panel {
  min-height: 360px;
  padding: 22px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-heading h2 {
  margin: 4px 0 0;
  color: #172033;
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans SC", Arial, sans-serif;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.35;
}

.list-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 16px;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #edf1f6;
}

.list-item:first-of-type {
  border-top: 0;
}

.item-main h3,
.goods-item h3 {
  margin: 0 0 8px;
  color: #172033;
  font-size: 16px;
}

.item-main p,
.goods-item p {
  display: flex;
  gap: 6px;
  align-items: center;
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.item-side {
  display: grid;
  gap: 5px;
  justify-items: end;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.item-side strong {
  color: #e15a2b;
}

.goods-grid {
  display: grid;
  gap: 12px;
}

.goods-item {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #edf1f6;
  border-radius: 8px;
}

.goods-cover {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  overflow: hidden;
  color: #93a4b7;
  background: #e9eef5;
  border-radius: 8px;
}

.goods-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-item strong {
  color: #e15a2b;
  white-space: nowrap;
}

.notice-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 0;
  border-top: 1px solid #edf1f6;
}

.notice-item:first-of-type {
  border-top: 0;
}

.notice-item h3 {
  margin: 0 0 8px;
  color: #172033;
  font-size: 15px;
  line-height: 1.35;
}

.notice-item p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 980px) {
  .hero,
  .content-grid,
  .merchant-dashboard {
    grid-template-columns: 1fr;
  }

  .hero-copy {
    padding: 38px 34px 0;
  }

  .hero-board {
    margin: 0 34px 34px;
  }

  .service-grid,
  .merchant-home .service-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 640px) {
  .home-container {
    padding: 16px;
  }

  .hero {
    gap: 18px;
  }

  .hero h1 {
    font-size: 30px;
  }

  .hero-copy {
    padding: 26px 20px 0;
  }

  .hero-board {
    margin: 0 20px 20px;
    padding: 16px;
  }

  .overview-panel {
    grid-template-columns: 1fr;
  }

  .section-panel {
    padding: 18px;
  }

  .service-grid,
  .list-item,
  .merchant-home .service-grid,
  .job-row {
    grid-template-columns: 1fr;
  }

  .service-card {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .service-arrow {
    display: none;
  }

  .item-side {
    justify-items: start;
  }

  .job-progress span {
    text-align: left;
  }

  .job-actions {
    justify-content: flex-start;
  }

  .goods-item {
    grid-template-columns: 52px minmax(0, 1fr);
  }

  .goods-item strong {
    grid-column: 2;
  }
}
</style>
