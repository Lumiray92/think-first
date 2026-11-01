import { ref } from 'vue'
import { getUser } from './auth.js'
export const historyRevision = ref(0)
const key = () => 'thinkfirst.conversations.' + (getUser()?.email || 'local')
export function readConversations() {
  historyRevision.value
  try { return JSON.parse(localStorage.getItem(key()) || '[]') } catch { return [] }
}
export function recordConversation(state, turn, message) {
  if (!state.idea) return
  const all = readConversations()
  const id = state.sessionId || state.conversationId || (state.conversationId = String(Date.now()))
  let item = all.find(item => item.id === id)
  if (!item) { item = { id, title: state.idea, entries: [], versions: [] }; all.unshift(item) }
  if (message) item.entries.push({ role: '你', text: message, at: Date.now() })
  if (turn) item.entries.push({ role: 'ThinkFirst', text: turn.question || turn.title || '', candidates: turn.candidates || [], summary: turn.summary || [], at: Date.now() })
  if (turn?.prd && item.versions.at(-1)?.markdown !== turn.prd) item.versions.push({ at: Date.now(), markdown: turn.prd })
  // 保存完整快照用于恢复界面，提交前仍由后端验证会话归属。
  item.snapshot = JSON.parse(JSON.stringify(state))
  item.updatedAt = Date.now()
  try { localStorage.setItem(key(), JSON.stringify(all)); historyRevision.value++ } catch { throw new Error('浏览器历史记录空间不足，请先导出文档') }
}

// 数据库记录为准，本地快照只作当前浏览器显示缓存，不能据此绕过归属验证。
export function mergeServerConversations(sessions) {
  const local = readConversations()
  const merged = sessions.map(session => {
    const cached = local.find(item => item.id === session.id)
    const snapshot = { ...session, sessionId: session.id, pending: session.pending, prdMarkdown: session.finalPrd || session.pending?.prd }
    return { id: session.id, title: session.idea, updatedAt: session.updatedAt, snapshot,
      entries: session.messages?.length ? session.messages : cached?.entries || (session.history || []).map(item => ({ role: '你', text: item.answer?.text || '', at: item.at })),
      versions: session.versions?.length ? session.versions : cached?.versions || (snapshot.prdMarkdown ? [{ at: session.updatedAt, markdown: snapshot.prdMarkdown }] : []) }
  })
  localStorage.setItem(key(), JSON.stringify(merged))
  historyRevision.value++
}
