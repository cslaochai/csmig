<template>
  <div class="login-page">
    <!-- 左侧品牌区：深蓝渐变 + 系统定位 -->
    <aside class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo-wrap">
          <img alt="医保结算前置系统" class="brand-logo" src="../../assets/main_logo.png"/>
        </div>
        <h1 class="brand-title">医保结算前置系统</h1>
        <p class="brand-sub">MIG GATEWAY · MEDICAL INSURANCE PRE-SETTLEMENT</p>

        <ul class="brand-features">
          <li class="feature-item">
            <span class="feature-icon"><el-icon><Lock/></el-icon></span>
            <span class="feature-text">国密加解密 · 证书认证</span>
          </li>
          <li class="feature-item">
            <span class="feature-icon"><el-icon><Document/></el-icon></span>
            <span class="feature-text">交易留痕 · 全量日志审计</span>
          </li>
          <li class="feature-item">
            <span class="feature-icon"><el-icon><Odometer/></el-icon></span>
            <span class="feature-text">实时监控 · 异常告警</span>
          </li>
          <li class="feature-item">
            <span class="feature-icon"><el-icon><Tickets/></el-icon></span>
            <span class="feature-text">对账结算 · 目录缓存</span>
          </li>
        </ul>
      </div>
      <p class="brand-footer">HIS ↔ 医保局平台 转发层 + 管理后台</p>
    </aside>

    <!-- 右侧登录区 -->
    <main class="login-main">
      <div class="login-card">
        <h2 class="login-title">账号登录</h2>
        <p class="login-desc">请使用系统管理员分配的账号登录</p>

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

      <p class="login-footer">医保结算前置系统 · 仅限授权人员使用 · V1.0.0</p>
    </main>
  </div>
</template>

<script setup>
import {onMounted, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {CircleCheck, Document, Lock, Odometer, Tickets, User} from '@element-plus/icons-vue'
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

// ===== 前端验证码（canvas 绘制，去掉易混淆字符） =====
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
      // 保存 token 和用户信息
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
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  background:
      radial-gradient(1100px 520px at 88% -12%, rgba(64, 178, 255, 0.22), transparent 60%),
      radial-gradient(900px 500px at -8% 108%, rgba(14, 148, 136, 0.28), transparent 62%),
      linear-gradient(135deg, #072C52 0%, #0B4F8A 55%, #1269B5 100%);
}

/* ===== 左侧品牌区 ===== */
.brand-panel {
  flex: 1 1 52%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 56px 96px;
  color: #fff;
}

.brand-inner {
  max-width: 420px;
  width: 100%;
}

.brand-logo-wrap {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.96);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 8px 28px rgba(4, 34, 74, 0.35);
  margin-bottom: 24px;
}

.brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-title {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 6px;
  margin: 0 0 12px;
  line-height: 1.2;
}

.brand-sub {
  font-size: 12px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.62);
  margin: 0 0 36px;
  word-break: keep-all;
}

.brand-features {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13.5px;
  color: rgba(255, 255, 255, 0.88);
  letter-spacing: 0.5px;
}

.feature-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: #8ED1FF;
  flex-shrink: 0;
}

.brand-footer {
  position: absolute;
  bottom: 28px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.45);
}

/* ===== 右侧登录区 ===== */
.login-main {
  flex: 0 0 46%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px 24px;
}

.login-card {
  width: 420px;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 18px 48px rgba(4, 34, 74, 0.28);
  padding: 40px 40px 28px;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #0B3D6E;
  margin: 0;
  letter-spacing: 2px;
}

.login-desc {
  font-size: 12.5px;
  color: #8CA6BC;
  margin: 6px 0 24px;
  letter-spacing: 0.5px;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  padding-left: 12px;
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
  border-radius: 10px;
  border: 1px solid #E3ECF5;
  cursor: pointer;
  flex-shrink: 0;
}

.login-button {
  width: 100%;
  height: 46px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #1269B5 0%, #0B4F8A 100%);
}

.login-button:hover {
  background: linear-gradient(135deg, #1a7bc9 0%, #0e5a99 100%);
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
  margin-top: 22px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 1px;
}

/* ===== 窄屏适配：隐藏品牌区，登录卡居中 ===== */
@media (max-width: 960px) {
  .brand-panel {
    display: none;
  }

  .login-main {
    flex: 1 1 auto;
    padding: 32px 20px;
  }
}
</style>
