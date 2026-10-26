<template>
  <div class="login-container">
    <!-- 顶部横幅：公司 logo + 医院名（与应用主框架 Header 同风格） -->
    <header class="login-banner">
      <div class="login-banner-logo-wrap">
        <img alt="长沙市麓康医院" class="login-banner-logo" src="../../assets/sidebar_logo.png"/>
      </div>
      <span class="login-banner-name">长沙市麓康医院</span>
    </header>

    <div class="login-card">
      <!-- 院徽 + 院名 -->
      <div class="login-header">
        <img alt="长沙市麓康医院" class="hospital-logo" src="../../assets/main_logo.png"/>
        <h1 class="hospital-name">长沙市麓康医院</h1>
        <p class="hospital-sub">医保结算前置平台 · MIP GATEWAY</p>
      </div>

      <div class="login-divider"></div>

      <!-- 登录表单 -->
      <el-form :model="loginForm" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input
              v-model="loginForm.username"
              :prefix-icon="User"
              class="login-input"
              placeholder="请输入用户名"
              size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-input
              v-model="loginForm.password"
              :prefix-icon="Lock"
              class="login-input"
              placeholder="请输入密码"
              show-password
              size="large"
              type="password"
          />
        </el-form-item>
        <el-form-item>
          <div class="captcha-row">
            <el-input
                v-model="loginForm.captcha"
                :prefix-icon="CircleCheck"
                class="captcha-input"
                maxlength="4"
                placeholder="请输入验证码"
                size="large"
            />
            <canvas
                ref="captchaCanvas"
                class="captcha-image"
                height="40"
                title="点击刷新验证码"
                width="100"
                @click="refreshCaptcha"
            ></canvas>
          </div>
        </el-form-item>
        <!-- 登录失败常驻提示：表单内联红条，直到下次尝试才清除 -->
        <el-form-item v-if="loginError">
          <el-alert
              :closable="false"
              :title="loginError"
              class="login-error-alert"
              show-icon
              type="error"
          />
        </el-form-item>
        <el-form-item>
          <el-button
              :loading="loading"
              class="login-button"
              size="large"
              type="primary"
              @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
        <el-form-item class="cred-item">
          <span class="cred-tip">测试账号：admin / 123456</span>
        </el-form-item>
      </el-form>
    </div>

    <p class="login-footer">长沙市麓康医院医保结算前置平台 · 仅限授权人员使用 · V1.0.0</p>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {CircleCheck, Lock, User} from '@element-plus/icons-vue'
import {login} from '@/api/system'
import {encryptPassword} from '@/lib/password'

const router = useRouter()

const loading = ref(false)
const loginError = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  captcha: '',
})

// ===== 前端验证码（与 cslk 登录页同款：canvas 绘制，去掉易混淆字符） =====
const AUTO_FILL_CAPTCHA = true
const captchaCanvas = ref(null)
let captchaCode = ''

const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  captchaCode = Array.from({length: 4}, () => chars.charAt(Math.floor(Math.random() * chars.length))).join('')
  if (AUTO_FILL_CAPTCHA) {
    loginForm.captcha = captchaCode
  }
  drawCaptcha()
}

const drawCaptcha = () => {
  const canvas = captchaCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const width = canvas.width
  const height = canvas.height

  ctx.fillStyle = '#EAF2F9'
  ctx.fillRect(0, 0, width, height)

  for (let i = 0; i < 4; i++) {
    ctx.strokeStyle = `rgba(18, 105, 181, ${0.15 + Math.random() * 0.25})`
    ctx.lineWidth = 1
    ctx.beginPath()
    ctx.moveTo(Math.random() * width, Math.random() * height)
    ctx.lineTo(Math.random() * width, Math.random() * height)
    ctx.stroke()
  }

  const colors = ['#1269B5', '#0E9488', '#0B4F8A', '#5B8DB8']
  for (let i = 0; i < captchaCode.length; i++) {
    ctx.save()
    ctx.font = `bold ${20 + Math.floor(Math.random() * 5)}px Arial`
    ctx.fillStyle = colors[Math.floor(Math.random() * colors.length)]
    ctx.textBaseline = 'middle'
    ctx.translate(12 + i * 20, height / 2 + (Math.random() - 0.5) * 8)
    ctx.rotate((Math.random() - 0.5) * 0.5)
    ctx.fillText(captchaCode[i], 0, 0)
    ctx.restore()
  }

  ctx.fillStyle = 'rgba(11, 79, 138, 0.35)'
  for (let i = 0; i < 30; i++) {
    ctx.fillRect(Math.random() * width, Math.random() * height, 1.5, 1.5)
  }
}

const refreshCaptcha = () => {
  generateCaptcha()
}

onMounted(() => {
  generateCaptcha()
})

const handleLogin = async () => {
  if (!loginForm.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return
  }
  if (!loginForm.captcha) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (loginForm.captcha.trim().toUpperCase() !== captchaCode) {
    ElMessage.error('验证码错误')
    refreshCaptcha()
    return
  }

  loading.value = true
  loginError.value = ''
  try {
    const password = await encryptPassword(loginForm.password)
    const res = await login({
      username: loginForm.username,
      password: password,
    })

    if (res.code === 200) {
      // 保存 token 和用户信息（与 cslk 同一套 localStorage 键）
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userId', res.data.userId)
      localStorage.setItem('username', res.data.username)
      localStorage.setItem('realName', res.data.realName)
      localStorage.setItem('currentRole', res.data.currentRole)

      ElMessage.success('登录成功')
      router.push('/')
    } else {
      loginError.value = res.message || '登录失败'
      refreshCaptcha()
    }
  } catch (error) {
    loginError.value = error.message || '登录失败，请检查用户名和密码'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #EAF2F9 0%, #DDEAF4 100%);
  padding: 88px 24px 24px;
}

.login-banner {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 24px;
  background: var(--his-header-bg);
  box-shadow: var(--his-chrome-shadow);
}

.login-banner-logo-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.login-banner-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.login-banner-name {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
  white-space: nowrap;
}

.login-card {
  width: 440px;
  background: #fff;
  border: 1px solid #E3ECF5;
  border-radius: 14px;
  box-shadow: 0 6px 24px rgba(11, 79, 138, 0.08);
  padding: 40px 40px 28px;
}

.login-header {
  text-align: center;
}

.hospital-logo {
  width: 88px;
  height: 88px;
  margin: 0 auto 12px;
  display: block;
}

.hospital-name {
  font-size: 24px;
  font-weight: 700;
  color: #0B4F8A;
  letter-spacing: 4px;
  margin: 0;
}

.hospital-sub {
  font-size: 12px;
  color: #8CA6BC;
  letter-spacing: 2px;
  margin: 6px 0 0;
}

.login-divider {
  height: 1px;
  background: #E9F0F7;
  margin: 24px 0 20px;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
}

.login-error-alert {
  width: 100%;
  font-weight: 600;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 100px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #E3ECF5;
  cursor: pointer;
  flex-shrink: 0;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  border-radius: 8px;
}

.cred-item {
  display: flex;
  justify-content: center;
  margin-top: -8px;
  margin-bottom: 0 !important;
}

.cred-tip {
  font-size: 12px;
  color: #9AAFC2;
  letter-spacing: 1px;
}

.login-footer {
  margin-top: 24px;
  font-size: 12px;
  color: #9AAFC2;
  letter-spacing: 1px;
}
</style>
