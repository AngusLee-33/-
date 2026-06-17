<template>
  <main class="admin-page">
    <section class="admin-header">
      <div>
        <span>管理员后台</span>
        <h1>平台内容审核与用户管理</h1>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新数据</el-button>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="用户管理" name="users">
        <section class="import-panel user-import-panel">
          <div class="import-header">
            <div>
              <h2>导入用户信息</h2>
              <p>上传 .xls 或 .xlsx 文件，系统会按列名自动匹配用户名、密码、姓名、手机号、邮箱和角色，导入用户默认全部为正常状态。</p>
            </div>
            <div class="import-actions">
              <el-button plain :loading="templateLoading" @click="downloadUserTemplate">下载导入模板</el-button>
              <el-button plain :loading="exportLoading" @click="exportUsers">导出用户信息</el-button>
              <el-upload
                accept=".xls,.xlsx"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleSpreadsheetChange"
              >
                <el-button type="primary" :loading="importLoading">选择并导入</el-button>
              </el-upload>
            </div>
          </div>

          <div v-if="importResult" class="import-result">
            <div class="import-meta">
              <span>文件：{{ importResult.filename }}</span>
              <span>工作表：{{ importResult.sheetName }}</span>
              <span>总行数：{{ importResult.rowCount }}</span>
              <span>成功：{{ importResult.successCount }}</span>
              <span>跳过：{{ importResult.skippedCount }}</span>
            </div>
            <el-table :data="importResult.rows" border max-height="300" empty-text="暂无导入内容">
              <el-table-column
                v-for="column in importResult.columns"
                :key="column"
                :prop="column"
                :label="column"
                min-width="140"
                show-overflow-tooltip
              />
            </el-table>
          </div>
        </section>

        <el-table :data="users" v-loading="loading" border>
          <el-table-column label="用户ID" width="120" align="center">
            <template #default="{ row }">
              <span class="user-id">{{ formatUserId(row.userId) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column label="手机号" min-width="130">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="角色" width="140">
            <template #default="{ row }">
              <el-select v-model="row.role" size="small" @change="updateUserRole(row, $event)">
                <el-option label="学生" value="student" />
                <el-option label="商家" value="merchant" />
                <el-option label="管理员" value="admin" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="userStatusType(row.status)">{{ userStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button size="small" plain @click="openPasswordDialog(row)">重置密码</el-button>
              <el-button
                size="small"
                :type="row.status === 'active' ? 'danger' : 'success'"
                plain
                @click="updateUserStatus(row, row.status === 'active' ? 'disabled' : 'active')"
              >
                {{ row.status === 'active' ? '禁用' : '启用' }}
              </el-button>
              <el-button size="small" type="danger" plain @click="deleteUser(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="拼车审核" name="carpools">
        <div class="audit-toolbar">
          <el-button
            type="danger"
            plain
            :disabled="!selectedCarpools.length"
            @click="deleteSelectedCarpools"
          >
            批量删除
          </el-button>
        </div>
        <el-table
          :data="carpools"
          v-loading="loading"
          border
          @selection-change="selectedCarpools = $event"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="carpoolId" label="ID" width="80" />
          <el-table-column label="路线" min-width="180">
            <template #default="{ row }">{{ row.departure }} → {{ row.destination }}</template>
          </el-table-column>
          <el-table-column label="发布人" min-width="120">
            <template #default="{ row }">{{ row.user?.realName || row.user?.username }}</template>
          </el-table-column>
          <el-table-column prop="departureTime" label="出发时间" min-width="160" :formatter="formatDateColumn" />
          <el-table-column label="拼车人数" width="160" align="center">
            <template #default="{ row }">{{ formatCarpoolPeople(row) }}</template>
          </el-table-column>
          <el-table-column label="价格" width="110">
            <template #default="{ row }">{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="updateCarpoolStatus(row, 'active')">通过</el-button>
              <el-button size="small" type="danger" plain @click="updateCarpoolStatus(row, 'canceled')">下架</el-button>
              <el-button size="small" type="danger" plain @click="deleteCarpool(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="兼职审核" name="partTimes">
        <div class="audit-toolbar">
          <el-button
            type="danger"
            plain
            :disabled="!selectedPartTimes.length"
            @click="deleteSelectedPartTimes"
          >
            批量删除
          </el-button>
        </div>
        <el-table
          :data="partTimes"
          v-loading="loading"
          border
          @selection-change="selectedPartTimes = $event"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="partTimeId" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column label="商家" min-width="120">
            <template #default="{ row }">{{ row.merchant?.realName || row.merchant?.username }}</template>
          </el-table-column>
          <el-table-column prop="location" label="地点" min-width="120" />
          <el-table-column label="薪资" min-width="150">
            <template #default="{ row }">{{ formatMoney(row.salaryMin) }} - {{ formatMoney(row.salaryMax) }}/小时</template>
          </el-table-column>
          <el-table-column label="招聘人数" width="110" align="center">
            <template #default="{ row }">{{ formatAcceptedCount(row) }}/{{ formatRecruitCount(row) }}人</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="updatePartTimeStatus(row, 'active')">通过</el-button>
              <el-button size="small" type="danger" plain @click="updatePartTimeStatus(row, 'closed')">关闭</el-button>
              <el-button size="small" type="danger" plain @click="deletePartTime(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="闲置审核" name="secondhands">
        <div class="audit-toolbar">
          <el-button
            type="danger"
            plain
            :disabled="!selectedSecondhands.length"
            @click="deleteSelectedSecondhands"
          >
            批量删除
          </el-button>
        </div>
        <el-table
          :data="secondhands"
          v-loading="loading"
          border
          @selection-change="selectedSecondhands = $event"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="secondhandId" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="category" label="分类" min-width="120" />
          <el-table-column label="发布人" min-width="120">
            <template #default="{ row }">{{ row.user?.realName || row.user?.username }}</template>
          </el-table-column>
          <el-table-column label="价格" width="110">
            <template #default="{ row }">{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="updateSecondhandStatus(row, 'active')">通过</el-button>
              <el-button size="small" type="danger" plain @click="updateSecondhandStatus(row, 'offline')">下架</el-button>
              <el-button size="small" type="danger" plain @click="deleteSecondhand(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="失物招领管理" name="lostFounds">
        <el-table :data="lostFounds" v-loading="loading" border>
          <el-table-column prop="lostFoundId" label="ID" width="80" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ lostFoundTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
          <el-table-column label="发布人" min-width="120">
            <template #default="{ row }">{{ row.user?.realName || row.user?.username || '-' }}</template>
          </el-table-column>
          <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
          <el-table-column prop="contact" label="联系方式" min-width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="lostFoundStatusType(row.status)">{{ lostFoundStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" min-width="160" :formatter="formatDateColumn" />
          <el-table-column label="操作" width="300" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" plain @click="updateLostFoundStatus(row, 'open')">开放</el-button>
              <el-button size="small" plain @click="updateLostFoundStatus(row, 'resolved')">解决</el-button>
              <el-button size="small" type="warning" plain @click="updateLostFoundStatus(row, 'closed')">关闭</el-button>
              <el-button size="small" type="danger" plain @click="deleteLostFound(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="订单管理" name="orders">
        <el-alert
          class="order-tip"
          title="发布闲置商品不会自动生成订单；订单需要由另一个买家账号在闲置交易页面点击“立即下单”后才会出现。"
          type="info"
          :closable="false"
          show-icon
        />
        <div class="table-toolbar">
          <el-select v-model="orderFilters.status" placeholder="订单状态" clearable>
            <el-option label="全部" value="" />
            <el-option label="待支付" value="created" />
            <el-option label="已支付" value="payed" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="canceled" />
          </el-select>
          <el-input v-model.trim="orderFilters.keyword" clearable placeholder="搜索订单号、商品、买家、卖家" />
          <el-button
            type="danger"
            plain
            :disabled="!selectedOrders.length"
            @click="deleteSelectedOrders"
          >
            批量删除
          </el-button>
        </div>
        <el-table
          :data="filteredOrders"
          v-loading="loading"
          border
          empty-text="暂无订单，请先在闲置交易页面创建订单"
          @selection-change="selectedOrders = $event"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column label="订单ID" width="120" align="center">
            <template #default="{ row }">
              <span class="user-id">{{ formatOrderId(row.orderId) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="商品" min-width="220">
            <template #default="{ row }">
              <div class="order-product">
                <img v-if="row.targetImage" :src="row.targetImage" alt="商品图片" />
                <span v-else class="order-product-empty">无图</span>
                <div>
                  <strong>{{ row.targetTitle || '闲置交易订单' }}</strong>
                  <small>内容ID：{{ row.targetId }}</small>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="买家" min-width="130">
            <template #default="{ row }">{{ row.user?.realName || row.user?.username || '-' }}</template>
          </el-table-column>
          <el-table-column label="卖家" min-width="130">
            <template #default="{ row }">{{ row.seller?.realName || row.seller?.username || '-' }}</template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">{{ orderTypeText(row.type) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" min-width="160" :formatter="formatDateColumn" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" plain @click="deleteOrder(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="备份管理" name="backup">
        <section class="backup-panel">
          <div class="backup-header">
            <div>
              <h2>数据库离线备份</h2>
              <p>生成 SQL 备份文件并自动下载到本机，可按需要选择只备份结构、只备份数据或完整备份。</p>
            </div>
            <el-button type="primary" :loading="backupLoading" @click="downloadBackup">立即备份</el-button>
          </div>
          <el-checkbox-group v-model="backupOptions" class="backup-options">
            <el-checkbox label="structure">备份表结构</el-checkbox>
            <el-checkbox label="data">备份表数据</el-checkbox>
          </el-checkbox-group>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="passwordDialogVisible" title="重置用户密码" width="420px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ passwordTarget?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model.trim="passwordForm.password"
            type="password"
            show-password
            maxlength="50"
            placeholder="请输入 6 到 50 位新密码"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model.trim="passwordForm.confirmPassword"
            type="password"
            show-password
            maxlength="50"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi, getErrorMessage } from '../api'

const activeTab = ref('users')
const loading = ref(false)
const summary = ref({})
const users = ref([])
const carpools = ref([])
const partTimes = ref([])
const secondhands = ref([])
const lostFounds = ref([])
const orders = ref([])
const selectedCarpools = ref([])
const selectedPartTimes = ref([])
const selectedSecondhands = ref([])
const selectedOrders = ref([])
const passwordDialogVisible = ref(false)
const passwordSaving = ref(false)
const backupLoading = ref(false)
const backupOptions = ref(['structure', 'data'])
const importLoading = ref(false)
const templateLoading = ref(false)
const exportLoading = ref(false)
const importResult = ref(null)
const passwordTarget = ref(null)
const passwordForm = reactive({
  password: '',
  confirmPassword: ''
})
const orderFilters = reactive({
  status: '',
  keyword: ''
})

const summaryCards = computed(() => [
  { label: '用户总数', value: summary.value.users || 0 },
  { label: '待审拼车', value: summary.value.pendingCarpools || 0 },
  { label: '待审兼职', value: summary.value.pendingPartTimes || 0 },
  { label: '待审闲置', value: summary.value.pendingSecondhands || 0 }
])

const filteredOrders = computed(() => {
  const text = orderFilters.keyword.trim().toLowerCase()
  return orders.value.filter(order => {
    const statusMatched = !orderFilters.status || order.status === orderFilters.status
    const textMatched = !text || [
      formatOrderId(order.orderId),
      order.targetTitle,
      order.user?.username,
      order.user?.realName,
      order.seller?.username,
      order.seller?.realName
    ].filter(Boolean).some(value => String(value).toLowerCase().includes(text))
    return statusMatched && textMatched
  })
})

const loadAll = async () => {
  loading.value = true
  try {
    const [summaryRes, userRes, carpoolRes, partTimeRes, secondhandRes, lostFoundRes, orderRes] = await Promise.all([
      adminApi.getSummary(),
      adminApi.getUsers(),
      adminApi.getCarpools(),
      adminApi.getPartTimes(),
      adminApi.getSecondhands(),
      adminApi.getLostFounds(),
      adminApi.getOrders()
    ])
    summary.value = summaryRes.data || {}
    users.value = userRes.data || []
    carpools.value = carpoolRes.data || []
    partTimes.value = partTimeRes.data || []
    secondhands.value = secondhandRes.data || []
    lostFounds.value = lostFoundRes.data || []
    orders.value = orderRes.data || []
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '后台数据加载失败'))
  } finally {
    loading.value = false
  }
}

const openPasswordDialog = (row) => {
  passwordTarget.value = row
  passwordForm.password = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const submitPassword = async () => {
  if (!passwordTarget.value) return
  if (passwordForm.password.length < 6 || passwordForm.password.length > 50) {
    ElMessage.warning('密码长度必须在 6 到 50 位之间')
    return
  }
  if (passwordForm.password !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  passwordSaving.value = true
  try {
    const response = await adminApi.updateUserPassword(passwordTarget.value.userId, passwordForm.password)
    if (response.code === 200) {
      ElMessage.success('用户密码已更新')
      passwordDialogVisible.value = false
      await loadAll()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '密码更新失败'))
  } finally {
    passwordSaving.value = false
  }
}

const updateUserStatus = async (row, status) => {
  await runAction(() => adminApi.updateUserStatus(row.userId, status), '用户状态已更新')
}

const updateUserRole = async (row, role) => {
  await runAction(() => adminApi.updateUserRole(row.userId, role), '用户角色已更新')
}

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.username}」吗？该用户发布的内容、申请、订单和评价也会被删除。`,
      '删除用户',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deleteUser(row.userId), '用户已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除用户失败'))
    }
  }
}

const downloadBackup = async () => {
  if (!backupOptions.value.length) {
    ElMessage.warning('请至少选择备份结构或备份数据')
    return
  }

  backupLoading.value = true
  try {
    const response = await adminApi.backupDatabase({
      includeStructure: backupOptions.value.includes('structure'),
      includeData: backupOptions.value.includes('data')
    })
    const filename = getDownloadFilename(response) || `campus_service_backup_${Date.now()}.sql`
    const blob = new Blob([response.data], { type: 'application/sql;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('数据库备份文件已开始下载')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '数据库备份失败'))
  } finally {
    backupLoading.value = false
  }
}

const handleSpreadsheetChange = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  const filename = file.name.toLowerCase()
  if (!filename.endsWith('.xls') && !filename.endsWith('.xlsx')) {
    ElMessage.warning('只能导入 .xls 或 .xlsx 文件')
    return
  }

  importLoading.value = true
  try {
    const response = await adminApi.importSpreadsheet(file)
    if (response.code === 200) {
      importResult.value = response.data
      ElMessage.success(`导入完成，成功 ${response.data.successCount} 行，跳过 ${response.data.skippedCount} 行`)
      await loadAll()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '导入电子表格失败'))
  } finally {
    importLoading.value = false
  }
}

const downloadUserTemplate = async () => {
  templateLoading.value = true
  try {
    const response = await adminApi.downloadUserImportTemplate()
    const filename = getDownloadFilename(response) || '用户信息导入模板.xlsx'
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('用户导入模板已开始下载')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '下载导入模板失败'))
  } finally {
    templateLoading.value = false
  }
}

const exportUsers = async () => {
  exportLoading.value = true
  try {
    const response = await adminApi.exportUsers()
    const filename = getDownloadFilename(response) || '用户信息导出.xlsx'
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('用户信息已开始导出')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '导出用户信息失败'))
  } finally {
    exportLoading.value = false
  }
}

const deleteOrder = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除订单「${formatOrderId(row.orderId)}」吗？删除后用户将无法查看该订单信息。`,
      '删除订单',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deleteOrder(row.orderId), '订单已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除订单失败'))
    }
  }
}

const deleteSelectedOrders = async () => {
  if (!selectedOrders.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedOrders.value.length} 个订单吗？删除后用户将无法查看这些订单信息。`,
      '批量删除订单',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await Promise.all(selectedOrders.value.map(order => adminApi.deleteOrder(order.orderId)))
    ElMessage.success('选中订单已删除')
    selectedOrders.value = []
    await loadAll()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '批量删除订单失败'))
      await loadAll()
    }
  }
}

const deleteCarpool = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除拼车「${row.departure} → ${row.destination}」吗？相关拼车申请也会被删除。`,
      '删除拼车',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deleteCarpool(row.carpoolId), '拼车信息已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除拼车失败'))
    }
  }
}

const deleteSelectedCarpools = async () => {
  if (!selectedCarpools.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedCarpools.value.length} 条拼车信息吗？相关拼车申请也会被删除。`,
      '批量删除拼车',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await Promise.all(selectedCarpools.value.map(item => adminApi.deleteCarpool(item.carpoolId)))
    ElMessage.success('选中拼车信息已删除')
    selectedCarpools.value = []
    await loadAll()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '批量删除拼车失败'))
      await loadAll()
    }
  }
}

const deletePartTime = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除兼职「${row.title}」吗？相关兼职申请也会被删除。`,
      '删除兼职',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deletePartTime(row.partTimeId), '兼职信息已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除兼职失败'))
    }
  }
}

const deleteSelectedPartTimes = async () => {
  if (!selectedPartTimes.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedPartTimes.value.length} 条兼职信息吗？相关兼职申请也会被删除。`,
      '批量删除兼职',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await Promise.all(selectedPartTimes.value.map(item => adminApi.deletePartTime(item.partTimeId)))
    ElMessage.success('选中兼职信息已删除')
    selectedPartTimes.value = []
    await loadAll()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '批量删除兼职失败'))
      await loadAll()
    }
  }
}

const deleteSecondhand = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除闲置「${row.title}」吗？相关订单、评价和聊天记录也会被删除。`,
      '删除闲置',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deleteSecondhand(row.secondhandId), '闲置信息已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除闲置失败'))
    }
  }
}

const deleteSelectedSecondhands = async () => {
  if (!selectedSecondhands.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedSecondhands.value.length} 条闲置信息吗？相关订单、评价和聊天记录也会被删除。`,
      '批量删除闲置',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await Promise.all(selectedSecondhands.value.map(item => adminApi.deleteSecondhand(item.secondhandId)))
    ElMessage.success('选中闲置信息已删除')
    selectedSecondhands.value = []
    await loadAll()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '批量删除闲置失败'))
      await loadAll()
    }
  }
}

const deleteLostFound = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除失物招领「${row.title}」吗？删除后用户端将无法查看该信息。`,
      '删除失物招领',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await runAction(() => adminApi.deleteLostFound(row.lostFoundId), '失物招领信息已删除')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除失物招领失败'))
    }
  }
}

const updateCarpoolStatus = async (row, status) => {
  await runAction(() => adminApi.updateCarpoolStatus(row.carpoolId, status), '拼车状态已更新')
}

const updatePartTimeStatus = async (row, status) => {
  await runAction(() => adminApi.updatePartTimeStatus(row.partTimeId, status), '兼职状态已更新')
}

const updateSecondhandStatus = async (row, status) => {
  await runAction(() => adminApi.updateSecondhandStatus(row.secondhandId, status), '闲置状态已更新')
}

const updateLostFoundStatus = async (row, status) => {
  await runAction(() => adminApi.updateLostFoundStatus(row.lostFoundId, status), '失物招领状态已更新')
}

const runAction = async (action, successMessage) => {
  try {
    const response = await action()
    response.code === 200 ? ElMessage.success(successMessage) : ElMessage.error(response.message)
    await loadAll()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '操作失败'))
    await loadAll()
  }
}

const statusText = (status) => ({
  pending: '待审核',
  active: '已通过',
  confirmed: '已确认',
  completed: '已完成',
  canceled: '已下架',
  closed: '已关闭',
  sold: '已售出',
  offline: '已下架'
}[status] || status || '未知')

const statusType = (status) => ({
  pending: 'warning',
  active: 'success',
  confirmed: 'success',
  completed: 'info',
  canceled: 'danger',
  closed: 'danger',
  sold: 'info',
  offline: 'danger'
}[status] || 'info')

const lostFoundTypeText = (type) => ({
  lost: '寻物',
  found: '招领'
}[type] || type || '未知')

const lostFoundStatusText = (status) => ({
  open: '开放中',
  resolved: '已解决',
  closed: '已关闭'
}[status] || status || '未知')

const lostFoundStatusType = (status) => ({
  open: 'success',
  resolved: 'info',
  closed: 'danger'
}[status] || 'info')

const orderTypeText = (type) => ({
  secondhand: '闲置交易'
}[type] || type || '未知')

const orderStatusText = (status) => ({
  created: '待支付',
  payed: '已支付',
  completed: '已完成',
  cancelled: '已取消',
  canceled: '已取消'
}[status] || status || '未知')

const orderStatusType = (status) => ({
  created: 'warning',
  payed: 'primary',
  completed: 'success',
  cancelled: 'info',
  canceled: 'info'
}[status] || 'info')

const userStatusText = (status) => ({
  active: '正常',
  disabled: '禁用'
}[status] || status || '未知')

const userStatusType = (status) => status === 'active' ? 'success' : 'danger'

const formatUserId = (id) => {
  return `U${String(id || 0).padStart(6, '0')}`
}

const formatOrderId = (id) => {
  return `O${String(id || 0).padStart(6, '0')}`
}

const formatMoney = (value) => {
  const amount = Number(value)
  return Number.isFinite(amount) ? `¥${amount.toFixed(2)}` : '¥0.00'
}

const formatCarpoolPeople = (row) => {
  const seats = Number(row?.seats)
  const acceptedCount = Number(row?.acceptedCount)
  const remainingSeats = Number.isFinite(seats) && seats >= 0 ? Math.floor(seats) : 0
  const joinedCount = Number.isFinite(acceptedCount) && acceptedCount >= 0 ? Math.floor(acceptedCount) : 0
  return `已拼${joinedCount}人 / 剩余${remainingSeats}座`
}

const formatRecruitCount = (row) => {
  const count = Number(row?.recruitCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 1
}

const formatAcceptedCount = (row) => {
  const count = Number(row?.acceptedCount)
  return Number.isFinite(count) && count > 0 ? Math.floor(count) : 0
}

const formatDateColumn = (row, column, value) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}

const getDownloadFilename = (response) => {
  const disposition = response.headers?.['content-disposition']
  if (!disposition) return ''
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match) {
    return decodeURIComponent(utf8Match[1])
  }
  const match = disposition.match(/filename="?([^"]+)"?/i)
  return match ? match[1] : ''
}

onMounted(loadAll)
</script>

<style scoped>
.admin-page {
  min-height: calc(100vh - 64px);
  padding: 28px;
  background: #f4f7fb;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  max-width: 1280px;
  margin: 0 auto 18px;
}

.admin-header span {
  color: #1f5eff;
  font-size: 13px;
  font-weight: 700;
}

.admin-header h1 {
  margin: 6px 0 0;
  color: #172033;
  font-size: 28px;
  letter-spacing: 0;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  max-width: 1280px;
  margin: 0 auto 18px;
}

.summary-card {
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.summary-card span {
  color: #64748b;
  font-size: 14px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  color: #172033;
  font-size: 30px;
}

.admin-tabs {
  max-width: 1280px;
  margin: 0 auto;
  padding: 18px;
  background: #ffffff;
  border: 1px solid #e6ebf2;
  border-radius: 8px;
}

.user-id {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 82px;
  height: 26px;
  color: #1f5eff;
  font-family: Consolas, Monaco, monospace;
  font-size: 13px;
  font-weight: 700;
  background: #edf4ff;
  border: 1px solid #d6e5ff;
  border-radius: 6px;
}

.table-toolbar {
  display: grid;
  grid-template-columns: 160px minmax(220px, 360px) auto;
  gap: 12px;
  margin-bottom: 14px;
}

.audit-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}

.order-tip {
  margin-bottom: 14px;
}

.backup-panel {
  padding: 22px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.backup-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.backup-header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  letter-spacing: 0;
}

.backup-header p {
  max-width: 680px;
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.backup-options {
  display: flex;
  gap: 18px;
  margin-top: 18px;
}

.import-panel {
  padding: 22px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.user-import-panel {
  margin-bottom: 16px;
}

.import-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 18px;
}

.import-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.import-header h2 {
  margin: 0;
  color: #172033;
  font-size: 20px;
  letter-spacing: 0;
}

.import-header p {
  max-width: 680px;
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.import-result {
  display: grid;
  gap: 14px;
}

.import-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.import-meta span {
  padding: 6px 10px;
  color: #475569;
  font-size: 13px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.order-product {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.order-product img,
.order-product-empty {
  width: 54px;
  height: 42px;
  border-radius: 6px;
}

.order-product img {
  object-fit: cover;
}

.order-product-empty {
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-size: 12px;
  background: #f1f5f9;
}

.order-product strong,
.order-product small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-product strong {
  color: #172033;
}

.order-product small {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 860px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .admin-page {
    padding: 16px;
  }

  .admin-header {
    display: grid;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
