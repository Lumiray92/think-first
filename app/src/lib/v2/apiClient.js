import { request } from '../auth.js'
import { createState } from './engine.js'

// 与 agentClient 相同的对外接口，但状态由 ThinkFirst 后端驱动。
// 后端返回 slots 快照，刷新页面后仍能恢复标题、PRD 和进度。

function applyView(state, view) {
  state.sessionId = view.sessionId
  state.mode = view.mode
  state.status = view.status
  state.slots = view.slots || {}
  state.updatedAt = Date.now()

  const turn = view.turn || null
  if (turn) {
    state.pending = turn
    if (turn.progress) {
      state.stage = turn.progress.stage
      state.group = turn.progress.group
    }
    if (turn.prd) state.prdMarkdown = turn.prd
  }
  return turn
}

function toAction(action = {}) {
  switch (action.type) {
    case 'revise':
      return { type: 'revise', text: String(action.text || '') }
    case 'polish':
      return { type: 'polish', text: String(action.text || '') }
    case 'regenerate':
      return { type: 'regenerate' }
    case 'confirm':
      return { type: 'confirm' }
    case 'back':
      return { type: 'back', stage: action.stage }
    case 'free':
      return { text: String(action.text || '') }
    default:
      return { index: Number.isInteger(action.index) ? action.index : 0 }
  }
}

async function start(state, { idea = '', mode = 'quick' } = {}) {
  Object.keys(state).forEach((key) => delete state[key])
  Object.assign(state, createState({ idea, mode }))

  const view = await request('/coach/session', {
    method: 'POST',
    body: { idea, mode: mode === 'full' ? 'full' : 'quick' },
  })
  return applyView(state, view)
}

async function act(state, action = {}) {
  if (!state.sessionId) {
    throw new Error('会话已过期，请重新开始')
  }
  const view = await request(`/coach/session/${state.sessionId}/turn`, {
    method: 'POST',
    body: toAction(action),
  })
  return applyView(state, view)
}

async function getPrd(state) {
  if (!state.sessionId) return state.prdMarkdown || ''
  const data = await request(`/coach/session/${state.sessionId}/prd`)
  return data.markdown || ''
}

export const apiClient = { start, act, getPrd }
