<template>
  <div class="login-page">
    <section class="login-shell">
      <aside class="brand-panel">
        <el-tag effect="dark" class="brand-tag">校园生活一站式入口</el-tag>
        <h1>校园综合服务平台</h1>
        <p>集中处理拼车、兼职、闲置交易和失物招领，让常用校园服务更快找到、更好管理。</p>

        <div class="feature-list">
          <div class="feature-item">
            <span><el-icon><Van /></el-icon></span>
            <div>
              <strong>拼车同行</strong>
              <small>快速查看路线、申请座位</small>
            </div>
          </div>
          <div class="feature-item">
            <span><el-icon><Briefcase /></el-icon></span>
            <div>
              <strong>兼职招聘</strong>
              <small>浏览岗位并提交申请</small>
            </div>
          </div>
          <div class="feature-item">
            <span><el-icon><Goods /></el-icon></span>
            <div>
              <strong>闲置交易</strong>
              <small>发布闲置，发现好物</small>
            </div>
          </div>
        </div>
      </aside>

      <main class="auth-panel">
        <div class="login-card">
          <div class="form-heading">
            <span class="form-icon"><el-icon><UserFilled /></el-icon></span>
            <div>
              <h2>欢迎登录</h2>
              <p>使用账号进入校园服务平台</p>
            </div>
          </div>

          <el-form :model="form" ref="formRef" label-position="top" class="login-form" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
              <el-input
                v-model.trim="form.username"
                :prefix-icon="User"
                size="large"
                placeholder="请输入用户名"
                clearable
              />
        </el-form-item>
        <el-form-item label="密码" prop="password">
              <el-input
                v-model.trim="form.password"
                :prefix-icon="Lock"
                type="password"
                size="large"
                placeholder="请输入密码"
                show-password
              />
        </el-form-item>

            <div class="form-options">
              <span>管理员也从这里登录</span>
              <el-button link type="primary" @click="openResetDialog">忘记密码？</el-button>
            </div>

            <el-form-item>
              <el-button type="primary" size="large" @click="handleLogin" class="login-btn">
                登录
                <el-icon><ArrowRight /></el-icon>
              </el-button>
        </el-form-item>
      </el-form>

          <div class="register-link">
            <span>还没有账号？</span>
            <el-button link type="primary" @click="router.push('/register')">立即注册</el-button>
          </div>
        </div>
      </main>
    </section>

    <el-dialog v-model="resetDialogVisible" title="手机验证码找回密码" width="460px" class="reset-dialog">
      <el-form :model="resetForm" label-position="top">
        <el-form-item label="手机号">
          <div class="code-row">
            <el-input
              v-model.trim="resetForm.phone"
              :prefix-icon="Phone"
              maxlength="20"
              placeholder="请输入注册手机号"
              clearable
            />
            <el-button type="primary" plain :disabled="codeCountdown > 0" :loading="sendingCode" @click="sendResetCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="验证码">
          <el-input v-model.trim="resetForm.code" :prefix-icon="Key" maxlength="6" placeholder="请输入 6 位验证码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model.trim="resetForm.newPassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            maxlength="50"
            placeholder="请输入 6 到 50 位新密码"
          />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input
            v-model.trim="resetForm.confirmPassword"
            :prefix-icon="Lock"
            type="password"
            show-password
            maxlength="50"
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="resetPassword">
          <el-icon><RefreshLeft /></el-icon>
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { authApi, getErrorMessage } from '../api'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Briefcase,
  Goods,
  Key,
  Lock,
  Phone,
  RefreshLeft,
  User,
  UserFilled,
  Van
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const resetDialogVisible = ref(false)
const sendingCode = ref(false)
const resettingPassword = ref(false)
const codeCountdown = ref(0)
let countdownTimer = null

const form = reactive({
  username: '',
  password: ''
})

const resetForm = reactive({
  phone: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

onMounted(() => {
  ElMessage.closeAll()
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }

  try {
    const response = await userStore.login(form.username, form.password)
    if (response.code === 200) {
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    const message = getErrorMessage(error, '登录失败，请重试')
    ElMessage.error(message)
  }
}

const openResetDialog = () => {
  resetDialogVisible.value = true
}

const sendResetCode = async () => {
  if (!resetForm.phone) {
    ElMessage.warning('请输入注册手机号')
    return
  }

  sendingCode.value = true
  try {
    const response = await authApi.sendResetPasswordCode(resetForm.phone)
    if (response.code === 200) {
      ElMessage.success(response.data ? `验证码已发送，演示验证码：${response.data}` : '验证码已发送到手机')
      startCountdown()
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '验证码发送失败'))
  } finally {
    sendingCode.value = false
  }
}

const resetPassword = async () => {
  const message = validateResetForm()
  if (message) {
    ElMessage.warning(message)
    return
  }

  resettingPassword.value = true
  try {
    const response = await authApi.resetPassword({
      phone: resetForm.phone,
      code: resetForm.code,
      newPassword: resetForm.newPassword
    })
    if (response.code === 200) {
      ElMessage.success('密码重置成功，请使用新密码登录')
      resetDialogVisible.value = false
      resetForm.phone = ''
      resetForm.code = ''
      resetForm.newPassword = ''
      resetForm.confirmPassword = ''
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '密码重置失败'))
  } finally {
    resettingPassword.value = false
  }
}

const validateResetForm = () => {
  if (!resetForm.phone) return '请输入手机号'
  if (!resetForm.code) return '请输入验证码'
  if (resetForm.newPassword.length < 6 || resetForm.newPassword.length > 50) {
    return '新密码长度必须在 6 到 50 位之间'
  }
  if (resetForm.newPassword !== resetForm.confirmPassword) {
    return '两次输入的密码不一致'
  }
  return ''
}

const startCountdown = () => {
  codeCountdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}
</script>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 28px;
  background:
    linear-gradient(125deg, rgba(47, 112, 255, 0.12) 0%, rgba(255, 255, 255, 0) 42%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
}

.login-shell {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 460px);
  width: min(1080px, 100%);
  min-height: 640px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid #e4eaf2;
  border-radius: 8px;
  box-shadow: 0 22px 56px rgba(31, 45, 61, 0.12);
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 56px;
  color: #ffffff;
  background:
    linear-gradient(rgba(23, 34, 53, 0.76), rgba(23, 34, 53, 0.76)),
    url("https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80") center/cover;
}

.brand-tag {
  width: fit-content;
  border: 0;
  background: #1f5eff;
}

.brand-panel h1 {
  max-width: 520px;
  margin: 22px 0 16px;
  font-size: 46px;
  font-weight: 700;
  line-height: 1.16;
  letter-spacing: 0;
}

.brand-panel p {
  max-width: 540px;
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: 17px;
  line-height: 1.8;
}

.feature-list {
  display: grid;
  gap: 14px;
  max-width: 460px;
  margin-top: 42px;
}

.feature-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 14px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  backdrop-filter: blur(8px);
}

.feature-item span,
.form-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
}

.feature-item span {
  color: #172033;
  background: #ffffff;
  font-size: 22px;
}

.feature-item strong,
.feature-item small {
  display: block;
}

.feature-item strong {
  margin-bottom: 4px;
  font-size: 16px;
}

.feature-item small {
  color: rgba(255, 255, 255, 0.76);
  font-size: 13px;
}

.auth-panel {
  display: grid;
  place-items: center;
  padding: 42px;
  background: #ffffff;
}

.login-card {
  width: 100%;
}

.form-heading {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 34px;
}

.form-icon {
  flex: 0 0 auto;
  color: #1f5eff;
  background: #eaf1ff;
  font-size: 24px;
}

.form-heading h2 {
  margin: 0 0 6px;
  color: #172033;
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: 0;
}

.form-heading p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.login-form :deep(.el-form-item__label) {
  color: #334155;
  font-weight: 600;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin: -4px 0 18px;
  color: #7a8797;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  border-radius: 8px;
  font-weight: 700;
}

.register-link {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 2px;
  margin-top: 12px;
  color: #64748b;
  font-size: 14px;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  width: 100%;
}

.reset-dialog :deep(.el-dialog) {
  border-radius: 8px;
}

@media (max-width: 860px) {
  .login-page {
    padding: 18px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    padding: 34px;
  }

  .brand-panel h1 {
    font-size: 34px;
  }

  .feature-list {
    margin-top: 28px;
  }

  .auth-panel {
    padding: 32px;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 0;
    place-items: stretch;
  }

  .login-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .brand-panel {
    padding: 28px 20px;
  }

  .brand-panel h1 {
    font-size: 28px;
  }

  .brand-panel p {
    font-size: 15px;
  }

  .auth-panel {
    padding: 28px 20px 34px;
  }

  .form-heading h2 {
    font-size: 26px;
  }

  .form-options,
  .register-link {
    align-items: flex-start;
    flex-direction: column;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
