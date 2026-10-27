import { reactive, ref } from 'vue'

import { STAGES } from '@/lib/questions'
import { readDraft, writeDraft } from '@/lib/storage'

function nowLabel() {
  return new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function clampStage(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return 0
  return Math.max(0, Math.min(Math.trunc(n), STAGES.length - 1))
}

// 整个应用共享同一份状态：切换路由不会丢，刷新后从 localStorage 恢复
const state = reactive(readDraft())
const currentStage = ref(clampStage(state.stage))
const savedAt = ref(nowLabel())

export function useSession() {
  function saveNow() {
    state.stage = currentStage.value
    writeDraft(state)
    savedAt.value = nowLabel()
  }

  function goStage(i) {
    if (i < 0 || i >= STAGES.length || i === currentStage.value) return
    currentStage.value = i
    saveNow()
  }

  function setMode(mode) {
    if (mode !== 'chat' && mode !== 'guide') return
    if (state.mode === mode) return
    state.mode = mode
    saveNow()
  }

  return { state, currentStage, savedAt, saveNow, goStage, setMode }
}
