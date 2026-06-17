<template>
  <div class="register-container">
    <el-card class="register-card" shadow="hover">
      <h2 class="title">校园综合服务平台</h2>
      <h3 class="subtitle">用户注册</h3>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model.trim="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model.trim="form.password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model.trim="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="学生" value="student" />
            <el-option label="商家" value="merchant" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" class="register-btn">注册</el-button>
        </el-form-item>
      </el-form>
      <p class="login-link">
        已有账号？<a href="/login">立即登录</a>
      </p>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getErrorMessage } from '../api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  role: 'student'
})

const handleRegister = async () => {
  if (!form.username || !form.password || !form.realName) {
    ElMessage.error('请填写必填项')
    return
  }

  try {
    const response = await userStore.register(form)
    if (response.code === 200) {
      ElMessage.success('注册成功')
      router.push('/login')
    } else {
      ElMessage.error(response.message)
    }
  } catch (error) {
    const message = getErrorMessage(error, '注册失败，请重试')
    ElMessage.error(message)
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 500px;
  padding: 40px;
  border-radius: 12px;
}

.title {
  text-align: center;
  color: #409EFF;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
}

.register-btn {
  width: 100%;
}

.login-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.login-link a {
  color: #409EFF;
  text-decoration: none;
}
</style>
