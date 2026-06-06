<template>
  <div class="login-page">
    <div class="login-card">
      <h2>企业日历系统</h2>
      <el-form :model="form" label-width="0" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="default" @click="showRegister = true" size="large" style="width: 100%">
            注册新账号
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 注册弹窗 -->
    <el-dialog v-model="showRegister" title="注册新账号" width="400px" append-to-body>
      <el-form :model="registerForm" label-width="80px">
        <el-form-item label="用户名" required>
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="registerForm.displayName" placeholder="请输入显示名称" />
        </el-form-item>
        <el-form-item label="密码" required>
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" @click="handleRegister" :loading="registering">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const showRegister = ref(false)
const registering = ref(false)
const registerForm = reactive({ username: '', password: '', displayName: '' })

async function handleLogin() {
  if (!form.username.trim() || !form.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/calendar')
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.username.trim() || !registerForm.password.trim()) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  registering.value = true
  try {
    await authStore.register(registerForm.username, registerForm.password, registerForm.displayName || registerForm.username)
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 360px;
  padding: 40px 32px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-card h2 {
  text-align: center;
  margin-bottom: 24px;
  font-size: 22px;
  color: #333;
}
</style>