import { EMPTY_STATE, LS_KEY } from './questions'

function clone(value) {
  return JSON.parse(JSON.stringify(value))
}

export function readDraft() {
  try {
    const raw = localStorage.getItem(LS_KEY)
    if (!raw) return clone(EMPTY_STATE)
    const s = JSON.parse(raw)
    const merged = clone(EMPTY_STATE)
    const answers =
      s.answers && typeof s.answers === 'object' && !Array.isArray(s.answers)
        ? { ...s.answers }
        : {}
    const chatAnswers =
      s.chatAnswers && typeof s.chatAnswers === 'object' && !Array.isArray(s.chatAnswers)
        ? { ...s.chatAnswers }
        : {}

    // 旧数据兼容：把 answers 里的 chat_* 迁到 chatAnswers
    Object.keys(answers).forEach((key) => {
      if (!key.startsWith('chat_')) return
      if (chatAnswers[key] == null) chatAnswers[key] = answers[key]
      delete answers[key]
    })

    merged.answers = answers
    merged.chatAnswers = chatAnswers

    if (s.mode === 'chat' || s.mode === 'guide') {
      merged.mode = s.mode
    } else if (Object.keys(chatAnswers).length && !Object.keys(answers).length) {
      merged.mode = 'chat'
    }

    if (typeof s.stage === 'number' && Number.isFinite(s.stage)) {
      merged.stage = s.stage
    }

    if (s.scores) merged.scores = Object.assign(merged.scores, s.scores)
    if (Array.isArray(s.features)) merged.features = s.features
    if (typeof s.focusRoi === 'string') merged.focusRoi = s.focusRoi
    if (s.selfcheck) merged.selfcheck = Object.assign(merged.selfcheck, s.selfcheck)
    if (s.adapt) merged.adapt = Object.assign(merged.adapt, s.adapt)
    if (s.chatHistory) merged.chatHistory = s.chatHistory
    if (typeof s.chatStep === 'number') merged.chatStep = s.chatStep
    if (s.chatLights) merged.chatLights = s.chatLights
    return merged
  } catch (e) {
    return clone(EMPTY_STATE)
  }
}

export function writeDraft(s) {
  try {
    localStorage.setItem(LS_KEY, JSON.stringify(s))
  } catch (e) {
    /* 本地存储不可用时静默失败，和原版一致 */
  }
}
