<template>
  <main class="orders-page">
    <section class="page-header">
      <div>
        <span>交易订单</span>
        <h1>我的订单</h1>
      </div>
      <el-button :loading="loading" @click="loadOrders">刷新</el-button>
    </section>

    <section class="filter-bar">
      <el-segmented v-model="statusFilter" :options="statusOptions" />
      <el-input v-model.trim="keyword" clearable placeholder="搜索商品、卖家或订单号" />
    </section>

    <section class="orders-list" v-loading="loading">
      <el-empty v-if="!filteredOrders.length" description="暂无订单" />
      <article v-for="order in filteredOrders" v-else :key="order.orderId" class="order-card">
        <div class="order-image">
          <img v-if="order.targetImage" :src="order.targetImage" alt="商品图片" />
          <span v-else>暂无图片</span>
        </div>
        <div class="order-main">
          <div class="order-title">
            <div>
              <h2>{{ order.targetTitle || '闲置交易订单' }}</h2>
              <p>{{ formatOrderId(order.orderId) }} · {{ formatTime(order.createTime) }}</p>
            </div>
            <el-tag :type="orderStatusType(order.status)">{{ orderStatusText(order.status) }}</el-tag>
          </div>
          <div class="order-meta">
            <span>卖家：{{ order.seller?.realName || order.seller?.username || '-' }}</span>
            <span>类型：{{ orderTypeText(order.type) }}</span>
            <span>金额：{{ formatMoney(order.amount) }}</span>
          </div>
          <div class="order-actions">
            <el-button
              type="primary"
              plain
              :disabled="order.status !== 'created'"
              :loading="actingId === order.orderId"
              @click="payOrder(order)"
            >
              模拟支付
            </el-button>
            <el-button
              type="success"
              plain
              :disabled="order.status !== 'payed'"
              :loading="actingId === order.orderId"
              @click="completeOrder(order)"
            >
              确认完成
            </el-button>
            <el-button
              type="danger"
              plain
              :disabled="order.status !== 'created'"
              :loading="actingId === order.orderId"
              @click="cancelOrder(order)"
            >
              取消订单
            </el-button>
          </div>
        </div>
      </article>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getErrorMessage, orderApi } from '../api'

const loading = ref(false)
const actingId = ref(null)
const orders = ref([])
const keyword = ref('')
const statusFilter = ref('')

const statusOptions = [
  { label: '全部', value: '' },
  { label: '待支付', value: 'created' },
  { label: '已支付', value: 'payed' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'canceled' }
]

const filteredOrders = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  return orders.value.filter(order => {
    const statusMatched = !statusFilter.value || order.status === statusFilter.value
    const textMatched = !text || [
      formatOrderId(order.orderId),
      order.targetTitle,
      order.seller?.username,
      order.seller?.realName
    ].filter(Boolean).some(value => String(value).toLowerCase().includes(text))
    return statusMatched && textMatched
  })
})

const loadOrders = async () => {
  loading.value = true
  try {
    const response = await orderApi.getMy()
    if (response.code === 200) {
      orders.value = response.data || []
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '订单加载失败'))
  } finally {
    loading.value = false
  }
}

const runOrderAction = async (order, action, message) => {
  actingId.value = order.orderId
  try {
    const response = await action(order.orderId)
    if (response.code === 200) {
      ElMessage.success(message)
      await loadOrders()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '订单操作失败'))
  } finally {
    actingId.value = null
  }
}

const payOrder = async (order) => {
  await runOrderAction(order, orderApi.pay, '支付成功')
}

const completeOrder = async (order) => {
  await runOrderAction(order, orderApi.complete, '订单已完成')
}

const cancelOrder = async (order) => {
  try {
    await ElMessageBox.confirm(`确定取消订单「${formatOrderId(order.orderId)}」吗？`, '取消订单', {
      confirmButtonText: '取消订单',
      cancelButtonText: '返回',
      type: 'warning'
    })
    await runOrderAction(order, orderApi.cancel, '订单已取消')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '取消订单失败'))
    }
  }
}

const orderTypeText = (type) => ({
  secondhand: '闲置交易'
}[type] || type || '未知')

const orderStatusText = (status) => ({
  created: '待支付',
  payed: '已支付',
  completed: '已完成',
  canceled: '已取消'
}[status] || status || '未知')

const orderStatusType = (status) => ({
  created: 'warning',
  payed: 'primary',
  completed: 'success',
  canceled: 'info'
}[status] || 'info')

const formatOrderId = (id) => `O${String(id || 0).padStart(6, '0')}`

const formatMoney = (value) => {
  const amount = Number(value)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(loadOrders)
</script>

<style scoped>
.orders-page {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.page-header,
.filter-bar,
.orders-list {
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
}

.filter-bar {
  display: grid;
  grid-template-columns: auto minmax(220px, 360px);
  gap: 14px;
  align-items: center;
  margin-bottom: 18px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.orders-list {
  display: grid;
  gap: 14px;
  min-height: 260px;
}

.order-card {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.order-image {
  display: grid;
  place-items: center;
  height: 120px;
  overflow: hidden;
  color: #94a3b8;
  background: #f1f5f9;
  border-radius: 8px;
}

.order-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-main {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.order-title {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.order-title h2 {
  margin: 0 0 6px;
  color: #172033;
  font-size: 19px;
}

.order-title p,
.order-meta {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.order-meta,
.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  align-items: center;
}

@media (max-width: 720px) {
  .orders-page {
    padding: 16px;
  }

  .filter-bar,
  .order-card {
    grid-template-columns: 1fr;
  }
}
</style>
