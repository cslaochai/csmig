import request from './request'

/**
 * 认证相关 API（与 cslk HIS 的 /auth/* 协议对齐）。
 */

/** 获取登录口令加密公钥（SM2） */
export function getPublicKey() {
    return request.get('/auth/publicKey')
}

/** 账号口令登录（password 为 SM2 加密后的密文） */
export function login(data) {
    return request.post('/auth/login', data)
}

/** 获取当前登录用户信息（顶栏用户区渲染） */
export function getUserInfo() {
    return request.get('/auth/info')
}

/** 退出登录 */
export function logout() {
    return request.post('/auth/logout')
}
