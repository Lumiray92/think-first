import { agentClient } from './agentClient.js'
import { apiClient } from './apiClient.js'

// 默认走本地 mock，演示时不依赖后端；
// 本地联调时设置 VITE_COACH_API=1。
const flag = String(import.meta.env.VITE_COACH_API || '').toLowerCase()
export const isRemote = flag !== '0' && flag !== 'false'

export const coachClient = isRemote ? apiClient : agentClient
