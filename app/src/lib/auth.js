// 登录态与用户接口。token 只存浏览器，Key 明文只在提交时经过内存。

const API_BASE = '/api/v1'
const TOKEN_KEY = 'tf.token'
const USER_KEY = 'tf.user'
// 登录令牌仅保存在当前标签会话，旧版长期存储令牌不再使用。
if (typeof window !== 'undefined') window.localStorage.removeItem(TOKEN_KEY)

function store() {
  try {
    return typeof window !== 'undefined' ? window.sessionStorage : null
  } catch (e) {
    return null
  }
}

export function getToken() {
  const s = store()
  return s ? s.getItem(TOKEN_KEY) || '' : ''
}

export function getUser() {
  const s = store()
  if (!s) return null
  try {
    return JSON.parse(s.getItem(USER_KEY) || 'null')
  } catch (e) {
    return null
  }
}

export function isLoggedIn() {
  return Boolean(getToken())
}

function saveUser(user) {
  const s = store()
  if (s) s.setItem(USER_KEY, JSON.stringify(user || {}))
}

export function applySession(data) {
  const s = store()
  if (!s || !data?.token) return
  s.setItem(TOKEN_KEY, data.token)
  saveUser(data.user)
}

export function clearSession() {
  const s = store()
  if (!s) return
  s.removeItem(TOKEN_KEY)
  s.removeItem(USER_KEY)
}

function redirectToLogin() {
  if (typeof window === 'undefined') return
  if (window.location.pathname.includes('/login')) return
  const base = import.meta.env.BASE_URL || '/'
  const target = window.location.pathname + window.location.search
  window.location.assign(`${base}login?redirect=${encodeURIComponent(target)}`)
}

/** 所有业务请求统一走这里：自动带 token，401 清登录态并回登录页。 */
export async function request(path, { method = 'GET', body, auth = true, redirectOn401 = true } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 270000)
  let response
  try {
    response = await fetch(API_BASE + path, {
      method,
      signal: controller.signal,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch (e) {
    clearTimeout(timer)
    if (e.name === 'AbortError') throw new Error('生成超时，请稍后重试')
    throw new Error('连不上后端，请确认 ThinkFirst 服务已启动')
  }

  clearTimeout(timer)
  if (response.status === 401 && auth) {
    clearSession()
    if (redirectOn401) redirectToLogin()
    throw new Error('登录已过期，请重新登录')
  }

  let payload = null
  try {
    payload = await response.json()
  } catch (e) {
    /* 非 JSON 响应按失败处理 */
  }
  if (!response.ok || payload?.code !== 200) {
    throw new Error(payload?.message || `请求失败（${response.status}）`)
  }
  return payload.data
}

export const authApi = {
  async register(email, password) {
    const data = await request('/auth/register', {
      method: 'POST',
      body: { email, password },
      auth: false,
      redirectOn401: false,
    })
    applySession(data)
    return data.user
  },

  async login(email, password) {
    const data = await request('/auth/login', {
      method: 'POST',
      body: { email, password },
      auth: false,
      redirectOn401: false,
    })
    applySession(data)
    return data.user
  },

  async me() {
    const user = await request('/auth/me')
    saveUser(user)
    return user
  },
}

export const apiKeyApi = {
  get: () => request('/user/api-key'),
  save: (apiKey) => request('/user/api-key', { method: 'PUT', body: { apiKey } }),
  validate: (apiKey) => request('/user/api-key/validate', { method: 'POST', body: { apiKey } }),
  remove: () => request('/user/api-key', { method: 'DELETE' }),
}
