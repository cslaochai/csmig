/**
 * 全局路由守卫 —— 登录态收口（对齐 cslk HIS 的 guard.js 思路，csmig 无菜单体系，只做登录拦截）。
 *
 * 判定口径：
 *  ① 登录页（/login）永远放行；
 *  ② 目标路由 meta.public（如 /login）放行；
 *  ③ 没有 token → 去 /login；
 *  ④ 有 token 但访问 /login → 回首页（避免登录后再回登录页）；
 *  ⑤ 其余放行（401 过期由 request.js 拦截器统一清 token 跳登录）。
 */
const PUBLIC_PATHS = ['/login']

function pathMatches(target, candidate) {
    if (candidate === '/') return target === '/'
    return target === candidate || target.startsWith(candidate + '/')
}

export function installRouteGuard(router, notify) {
    router.beforeEach((to) => {
        // ① 登录页永远放行
        if (PUBLIC_PATHS.some(p => pathMatches(to.path, p))) return true

        const token = localStorage.getItem('token')

        // ② 没有 token：去登录页
        if (!token) {
            notify?.('请先登录')
            return { path: '/login', replace: true }
        }

        // ③ 已登录访问登录页：回首页
        if (pathMatches(to.path, '/login')) {
            return { path: '/', replace: true }
        }

        return true
    })
}
