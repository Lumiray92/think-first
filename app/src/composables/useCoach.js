import { request, getUser } from '@/lib/auth.js'
import { recordConversation } from '@/lib/conversationHistory.js'
import { computed, reactive, ref } from 'vue'

import { coachClient } from '@/lib/v2/client.js'
import { createState } from '@/lib/v2/engine.js'
import { buildPrd } from '@/lib/v2/prd.js'
import { STAGES, TOTAL_STAGES, groupOfStage } from '@/lib/v2/schema.js'

const KEY = 'thinkfirst.v2'

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

function loadState() {
  if (typeof localStorage === 'undefined') return createState()
  try {
    const raw = localStorage.getItem(KEY)
    if (!raw) return createState()
    const parsed = JSON.parse(raw)
    if (parsed?.ownerAccount && parsed.ownerAccount !== getUser()?.email) return createState()
    if (!parsed || parsed.version !== 2) return createState()
    return { ...createState(), ...parsed, slots: parsed.slots || {} }
  } catch (e) {
    return createState()
  }
}

function nowLabel() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const state = reactive(loadState())
const turn = ref(state.pending ? clone(state.pending) : null)
const thinking = ref(false)
const error = ref('')
const savedAt = ref(nowLabel())

export function useCoach() {
  function save() {
    if (typeof localStorage === 'undefined') return
    try {
      state.ownerAccount = getUser()?.email || ''
      localStorage.setItem(KEY, JSON.stringify(state))
      savedAt.value = nowLabel()
    } catch (e) {
      /* 本地存储不可用时静默失败 */
    }
  }

  async function run(fn, message = '') {
    if (thinking.value) return null
    thinking.value = true
    error.value = ''
    try {
      const result = await fn()
      turn.value = result ? clone(result) : null
      save()
      recordConversation(state, result, message)
      return result
    } catch (e) {
      error.value = e?.message || '生成失败，请重试'
      return null
    } finally {
      thinking.value = false
    }
  }

  function start(idea, mode = 'quick') {
    return run(() => coachClient.start(state, { idea, mode }), idea)
  }

  function select(index) {
    return run(() => coachClient.act(state, { type: 'select', index }), turn.value?.candidates?.[index]?.text || '')
  }

  function free(text) {
    if (!String(text || '').trim()) return Promise.resolve(null)
    return run(() => coachClient.act(state, { type: 'free', text }), text)
  }

  function regenerate() {
    return run(() => coachClient.act(state, { type: 'regenerate' }))
  }

  function confirm() {
    return run(() => coachClient.act(state, { type: 'confirm' }), '确认并继续')
  }

  async function resume(snapshot) {
    if (!snapshot) return false
    return run(async () => {
      let restored = null
      if (snapshot.sessionId) {
        try { restored = await request(`/coach/session/${snapshot.sessionId}`) }
        catch (e) { throw new Error('这段会话暂时无法恢复，请检查服务连接或重新登录。' + (e.message || '')) }
      }
      Object.keys(state).forEach(key => delete state[key])
      Object.assign(state, clone(snapshot))
      // 优先使用服务端最新状态，避免多窗口操作后恢复旧卡片。
      if (restored) Object.assign(state, { mode: restored.mode, status: restored.status, slots: restored.slots, pending: restored.turn, stage: restored.turn?.progress?.stage || state.stage, group: restored.turn?.progress?.group || state.group, prdMarkdown: restored.turn?.prd || state.prdMarkdown })
      return state.pending
    })
  }
  function revise(text) {
    return run(() => coachClient.act(state, { type: 'revise', text }), '修改需求：' + text)
  }
  function polish(instructions) {
    return run(() => coachClient.act(state, { type: 'polish', text: instructions }), '调整文档：' + instructions)
  }

  function backTo(stage) {
    return run(() => coachClient.act(state, { type: 'back', stage }))
  }

  function reset() {
    Object.assign(state, createState())
    delete state.sessionId
    delete state.prdMarkdown
    turn.value = null
    error.value = ''
    save()
  }

  const prd = computed(() => state.prdMarkdown || buildPrd(state.slots, state))
  const progress = computed(
    () =>
      turn.value?.progress || {
        stage: state.stage,
        total: TOTAL_STAGES,
        group: groupOfStage(state.stage).id,
        groups: 5,
      },
  )
  const hasStarted = computed(() => state.status !== 'idle')
  const isDone = computed(() => state.status === 'done')
  const isBlocked = computed(() => state.status === 'blocked')
  const isConfirming = computed(() => state.status === 'confirming')
  const stageList = computed(() => STAGES)

  return {
    state,
    turn,
    thinking,
    error,
    savedAt,
    prd,
    progress,
    hasStarted,
    isDone,
    isBlocked,
    isConfirming,
    stageList,
    start,
    select,
    free,
    regenerate,
    polish,
    resume,
    revise,
    confirm,
    backTo,
    reset,
    save,
  }
}
