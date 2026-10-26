import axios from 'axios'
import {ElMessage} from 'element-plus'

// 前置机后端 context-path 为 /mip（vite 代理 /mip → http://localhost:8091）
const request = axios.create({
    baseURL: '/mip',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 401 整页跳转去登录页的单次闸门
let redirectingToLogin = false

// 全局错误提示的同文案节流：一次操作常并发多个请求，被同一原因挡下时拦截器会对每个
// 失败各弹一次。同文案在时间窗内只弹一次。
let lastErrorMessage = ''
let lastErrorAt = 0
const ERROR_DEDUP_MS = 2000

function notifyError(message) {
    const text = message || '操作失败，请稍后重试'
    const now = Date.now()
    if (text === lastErrorMessage && now - lastErrorAt < ERROR_DEDUP_MS) {
        return
    }
    lastErrorMessage = text
    lastErrorAt = now
    ElMessage.error(text)
}

// HTTP 状态码兜底文案
function httpStatusFallback(status) {
    const map = {
        400: '请求参数不正确',
        403: '没有权限访问',
        404: '请求的资源不存在',
        405: '请求方法不支持',
        415: '请求媒体类型不支持',
        500: '系统繁忙，请稍后重试',
        502: '服务暂时不可用，请稍后重试',
        503: '服务暂时不可用，请稍后重试',
        504: '服务暂时不可用，请稍后重试'
    }
    return map[status] || `请求失败（${status}）`
}

// 请求拦截器：注入登录令牌
request.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 响应拦截器：统一处理业务码与 HTTP 错误；{ silent: true } 的请求不打扰全局提示
request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            const message = res.message || '操作失败'
            const isLoginRequest = (response.config?.url || '').includes('/auth/login')
            if (!response.config?.silent && !isLoginRequest) {
                notifyError(message)
            }
            const err = new Error(message)
            err.code = res.code
            return Promise.reject(err)
        }
        return res
    },
    error => {
        const config = error.config || {}
        const silent = !!config.silent
        const isLoginRequest = (config.url || '').includes('/auth/login')

        if (error.code === 'ERR_CANCELED' || error.name === 'CanceledError' || axios.isCancel?.(error)) {
            return Promise.reject(error)
        }

        const {response} = error

        if (response) {
            const {status, data} = response
            const message = data?.message

            // 401：未登录/登录过期。非登录请求清 token 回登录页（单闸门，不额外弹框）
            if (status === 401) {
                if (!isLoginRequest) {
                    localStorage.removeItem('token')
                    if (!redirectingToLogin) {
                        redirectingToLogin = true
                        window.location.href = '/login'
                    }
                }
                return Promise.reject(error)
            }

            if (!silent) {
                notifyError(message || httpStatusFallback(status))
            }
            return Promise.reject(error)
        }

        // 请求未到达后端：超时 / 断网 / 跨域 / 后端未启动
        if (!silent) {
            if (error.code === 'ECONNABORTED') {
                notifyError('请求超时，请稍后重试')
            } else {
                notifyError('网络异常，请检查网络连接')
            }
        }
        return Promise.reject(error)
    }
)

export default request
