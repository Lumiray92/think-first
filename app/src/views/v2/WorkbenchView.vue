<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'

import { readConversations, mergeServerConversations } from '@/lib/conversationHistory.js'
import PrdMascot from '@/components/PrdMascot.vue'
import BrandLogo from '@/components/BrandLogo.vue'
import CandidateCards from '@/components/v2/CandidateCards.vue'
import ConfirmCard from '@/components/v2/ConfirmCard.vue'
import GateBanner from '@/components/v2/GateBanner.vue'
import InputBar from '@/components/v2/InputBar.vue'
import PrdPanel from '@/components/v2/PrdPanel.vue'
import StageRail from '@/components/v2/StageRail.vue'
import { useCoach } from '@/composables/useCoach'
import { authApi, clearSession, getUser, request } from '@/lib/auth.js'
import { isRemote } from '@/lib/v2/client.js'
import { groupById, stageByNo } from '@/lib/v2/schema.js'
import '@/styles/v2.css'
import '@/styles/readability.css'

const {
  state,
  turn,
  thinking,
  error,
  savedAt,
  prd,
  hasStarted,
  isDone,
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
} = useCoach()

const showAssistant = ref(false)
const revisionText = ref('')
const selectedSection = ref('')
// 从当前文档提取章节，编辑时让用户能对照原文定位。
const documentSections = computed(() => [...prd.value.matchAll(/^##\s+(.+)$/gm)].map(match => match[1].trim()))
function openAssistant() {
  showPrd.value = false
  showAssistant.value = true
  selectedSection.value = documentSections.value[0] || ''
}
const continuationKey = ref(0)
// 已完成只表示初稿已产出，仍允许在同一会话中追加修改并保存新版本。
async function continueConversation(text) {
  if (!text.trim() || thinking.value) return
  if (await revise(text.trim())) {
    continuationKey.value++
    showToast('需求文档已更新')
  }
}
async function revisePrd() {
  if (!revisionText.value.trim() || thinking.value) return
  if (await revise(`仅优化章节【${selectedSection.value || '用户指定段落'}】，其他内容保持不变。修改要求：${revisionText.value.trim()}`)) {
    revisionText.value = ''
    showAssistant.value = true
    showPrd.value = false
    showToast('需求已更新')
  }
}
async function resumeHistory(item) {
  const result = await resume(item.snapshot)
  if (result) {
    idea.value = state.idea
    mode.value = state.mode
    showHistory.value = false
    selectedHistory.value = null
  }
}
const showHistory = ref(false)
const historyError = ref('')
async function openHistory() {
  showHistory.value = true
  historyError.value = ''
  if (!isRemote) return
  try { mergeServerConversations(await request('/coach/session')) }
  catch (e) { historyError.value = e.message || '历史记录读取失败' }
}
const selectedHistory = ref(null)
const conversations = computed(() => readConversations())
const polishFormat = ref('公司详细版需求文档')
const polishRequirements = ref('')
const polishError = ref('')
async function polishDocument() {
  if (!isRemote) { polishError.value = '请连接 AI 服务后再调整文档'; return }
  polishError.value = ''
  const result = await polish(`目标格式：${polishFormat.value}\n新增要求：${polishRequirements.value.trim() || '保持现有需求，完善表达与结构'}`)
  if (result) showPrd.value = true
}
const router = useRouter()
const idea = ref(state.idea || '')
const mode = ref(state.mode || 'quick')
const inputRef = ref(null)
const showPrd = ref(false)
const toast = ref('')
const storedAccount = getUser()
const account = ref(storedAccount?.email ? storedAccount : null)

onMounted(async () => {
  if (!isRemote) return
  try {
    account.value = await authApi.me()
  } catch (e) {
    /* 401 已由请求层清理登录态并跳转 */
  }
})

const examples = [
  '做一个提醒妈妈吃药的小程序，她只会用微信',
  '每天把微信、便签里的待办抄到纸上，想做个打开即记的工具',
  '每周五收周报要花 40 分钟，还容易漏人',
]

const backOptions = computed(() => state.mode === 'quick'
  ? [1, 2, 3, 4, 5].map(groupById).filter(group => group.id <= currentGroup.value).map(group => ({ value: group.stages[0], label: `第 ${group.id} 组 · ${group.title}` }))
  : stageList.value.filter(stage => stage.no <= currentStage.value).map(stage => ({ value: stage.no, label: `第 ${stage.no} 步 · ${stage.name}` })))
function returnToStage(event) {
  const value = Number(event.target.value)
  event.target.value = ''
  if (value && !thinking.value) onStageSelect(value)
}
const currentStage = computed(() => Number(turn.value?.stage || state.stage) || 1)
const currentGroup = computed(() => Number(turn.value?.group || state.group) || 1)
const prdTitleText = computed(() => state.slots?.s1_title || '需求文档')
const canStart = computed(() => Boolean(idea.value.trim()) && !thinking.value)
const placeholder = computed(() => turn.value?.hint || '说一句就行，剩下的交给智能体…')

const recentHistory = computed(() =>
  state.history.map((item) => ({
    key: `${item.stage || 'g'}-${item.at}`,
    label: item.stage ? stageByNo(item.stage).name : groupById(item.group).title,
    text: item.answer?.text || '',
  })),
)

function showToast(message) {
  toast.value = message

}

async function startRun() {
  if (!canStart.value) return
  const result = await start(idea.value.trim(), mode.value)
  if (result) showPrd.value = false
}

function onSelect(index) {
  select(index)
}

function onFree(text) {
  free(text)
}

function onRegenerate() {
  regenerate()
}

function onConfirm() {
  confirm()
}

function onRetry() {
  if (state.mode === 'quick') backTo(groupById(currentGroup.value).stages[0])
  else backTo(currentStage.value)
}

function onGateBack(stage) {
  backTo(stage)
}

function onGateContinue() {
  confirm()
}

function onStageSelect(stageNo) {
  backTo(stageNo)
}

function onFreeFocus() {
  inputRef.value?.focus()
}

function onReset() {
  reset()
  idea.value = ''
  mode.value = 'quick'
  showPrd.value = false
}

function onLogout() {
  reset()
  clearSession()
  router.push({ name: 'login' })
}

async function copyPrd() {
  try {
    await navigator.clipboard.writeText(prd.value)
    showToast('PRD 已复制')
  } catch (e) {
    showToast('复制失败，请手动选择文本')
  }
}

function downloadPrd() {
  const blob = new Blob([prd.value], { type: 'text/markdown;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = prdTitleText.value + '.md'
  anchor.click()
  URL.revokeObjectURL(url)
  showToast('已导出 Markdown')
}
</script>

<template>
  <div class="agent-shell">
    <header class="agent-header">
      <BrandLogo />
      <nav>
        <button class="btn btn-quiet" @click="openHistory">历史对话</button>
        <button v-if="hasStarted" class="btn btn-quiet" @click="showPrd = !showPrd">需求文档</button>
        <RouterLink v-if="isRemote" class="btn btn-quiet" to="/settings">设置</RouterLink>
        <button v-if="hasStarted" class="btn btn-quiet" @click="onReset">新对话</button>
        <button v-if="isRemote && account" class="btn btn-quiet" @click="onLogout">退出</button>
      </nav>
    </header>
    <main class="agent-conversation" :aria-busy="thinking">
      <template v-if="!hasStarted">
        <div class="agent-intro"><h1>你想做什么？</h1><p>一起把想法变成需求。</p></div>
        <section class="agent-card">
          <textarea v-model="idea" class="agent-idea" rows="4" maxlength="200" placeholder="描述你的想法…" aria-label="你的想法" @keydown.ctrl.enter.prevent="startRun" @keydown.meta.enter.prevent="startRun" />
          <div class="agent-start-actions"><select v-model="mode" aria-label="引导模式"><option value="quick">快速梳理（5 组）</option><option value="full">逐步深入（12 步）</option></select><button class="btn btn-primary" :disabled="!canStart" @click="startRun">{{ thinking ? '准备中…' : '开始对话 →' }}</button></div>
        </section>
      </template>
      <template v-else>
        <div class="agent-user">{{ state.idea }}</div>
        <details v-if="recentHistory.length" class="agent-history"><summary>已完成的回答</summary><div v-for="item in recentHistory" :key="item.key" class="agent-user"><small>{{ item.label }}</small>{{ item.text }}</div></details>
        <div class="stage-back-controls">
          <select id="return-stage" aria-label="回到之前的阶段" :disabled="thinking" @change="returnToStage"><option value="">选择已到过的阶段</option><option v-for="option in backOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select>
        </div>
        <div class="agent-speaker"><PrdMascot @chat="openAssistant" /><span>ThinkFirst</span><span v-if="state.mode === 'full'" class="agent-stage">{{ currentStage === 2 ? 'JTBD 分析：用户想完成什么' : stageByNo(currentStage).name }}</span><span class="agent-progress">{{ state.mode === 'quick' ? `快速梳理 · 第 ${currentGroup} / 5 组` : `逐步深入 · 第 ${currentStage} / 12 步` }}</span></div>
        <div v-if="thinking" class="agent-card agent-loading" role="status">正在思考<span>···</span></div>
        <Transition v-else name="card" mode="out-in">
          <section v-if="turn" :key="`${currentStage}-${turn.kind}-${currentGroup}`" class="agent-card">
            <h2>{{ turn.kind === 'question' ? turn.question : turn.title }}</h2>
            <CandidateCards v-if="turn.kind === 'question'" :candidates="turn.candidates" :disabled="thinking" @select="onSelect" @regenerate="onRegenerate" @free="onFreeFocus" />
            <InputBar v-if="turn.kind === 'question'" ref="inputRef" :disabled="thinking" placeholder="或填写你的回答…" @submit="onFree" />
            <ConfirmCard v-else-if="turn.kind === 'confirm'" :turn="turn" :disabled="thinking" @confirm="onConfirm" @retry="onRetry" @edit="onFree" />
            <GateBanner v-else-if="turn.kind === 'gate'" :turn="turn" :disabled="thinking" @back="onGateBack" @continue="onGateContinue" />
            <div v-else-if="turn.kind === 'done'">
              <h3>继续修改需求</h3>
              <InputBar :key="continuationKey" :disabled="thinking" placeholder="告诉我还想改什么，例如：增加提醒时间设置…" @submit="continueConversation" />
              <div class="document-polish">
                <h3>把文档调整成你需要的样子</h3>
                <select v-model="polishFormat" aria-label="文档用途" :disabled="thinking"><option>公司详细版需求文档</option><option>适合 Codex 的开发提示词</option><option>适合 Claude Code 的开发提示词</option><option>按我的要求调整</option></select>
                <textarea v-model="polishRequirements" rows="3" maxlength="5000" aria-label="新增要求" placeholder="补充要求，例如：增加后台管理功能，详细说明异常情况…" :disabled="thinking" />
                <button class="btn btn-primary" :disabled="thinking" @click="polishDocument">生成新版本</button>
                <p v-if="polishError" role="alert">{{ polishError }}</p>
              </div>
              <div class="v2-actions"><button class="btn btn-primary" @click="downloadPrd">导出需求文档</button><button class="btn btn-ghost" @click="copyPrd">复制</button></div></div>
          </section>
        </Transition>
      </template>
      <div v-if="error" class="v2-error" role="alert">{{ error }} <span>请重新选择或提交回答。</span></div>
      <div v-if="toast" class="agent-feedback" role="status"><span>{{ toast }}</span><button class="btn btn-quiet" aria-label="关闭提示" @click="toast = ''">关闭</button></div>
    </main>
    <div v-if="showPrd || showAssistant" class="agent-overlay document-overlay" @click.self="showPrd = false; showAssistant = false" @keydown.esc="showPrd = false; showAssistant = false">
      <section class="document-workspace" :class="{ 'with-editor': showAssistant }" role="dialog" aria-modal="true" aria-label="需求文档">
        <aside v-if="showAssistant" class="section-editor">
          <h2>优化内容</h2>
          <label for="edit-section">选择章节</label>
          <select id="edit-section" v-model="selectedSection" :disabled="thinking"><option v-for="section in documentSections" :key="section" :value="section">{{ section }}</option></select>
          <textarea v-model="revisionText" class="assistant-input" rows="6" maxlength="4500" placeholder="想怎么改？也可以粘贴需要优化的那段文字。" aria-label="优化要求" :disabled="thinking" />
          <button class="btn btn-primary" :disabled="thinking || !revisionText.trim() || !hasStarted || !selectedSection" @click="revisePrd">{{ thinking ? '正在更新…' : '优化这段' }}</button>
          <p v-if="error" class="v2-error" role="alert">{{ error }}</p>
          <p v-if="toast" class="v2-auth-ok" role="status">{{ toast }}</p>
        </aside>
        <PrdPanel class="is-open" :markdown="prd" :title="prdTitleText" :busy="thinking" @copy="copyPrd" @download="downloadPrd" @close="showPrd = false; showAssistant = false" />
      </section>
    </div>
    <div v-if="showHistory" class="agent-overlay" @click.self="showHistory = false">
      <aside class="conversation-panel">
        <div class="v2-actions"><h2>历史对话</h2><button class="btn btn-quiet" @click="showHistory = false">关闭</button></div>
        <p v-if="historyError" class="v2-error" role="alert">{{ historyError }}</p><p>对话已保存到数据库，未完成的对话可继续。</p>
        <p v-if="error" class="v2-error" role="alert">{{ error }}</p>
        <template v-if="!selectedHistory">
          <p v-if="!conversations.length">还没有对话记录</p>
          <button v-for="item in conversations" :key="item.id" class="history-entry" @click="selectedHistory = item"><strong>{{ item.title }}</strong><small>{{ new Date(item.updatedAt).toLocaleString() }}</small></button>
        </template>
        <template v-else>
          <button class="btn btn-quiet" @click="selectedHistory = null">返回列表</button>
          <button v-if="selectedHistory.snapshot" class="btn btn-primary" :disabled="thinking" @click="resumeHistory(selectedHistory)">{{ selectedHistory.snapshot.status === 'done' ? '继续对话' : '继续完成' }}</button>
          <article v-for="(entry, index) in selectedHistory.entries" :key="index" class="history-message"><small>{{ entry.role }}</small><p>{{ entry.text }}</p><p v-for="(candidate, i) in entry.candidates" :key="i">{{ String.fromCharCode(65 + i) }} · {{ candidate.text }}</p><p v-for="row in entry.summary" :key="row.label">{{ row.label }}：{{ row.value }}</p></article>
          <details v-for="(version, index) in selectedHistory.versions" :key="index"><summary>文档版本 {{ index + 1 }} · {{ new Date(version.at).toLocaleString() }}</summary><pre class="history-document">{{ version.markdown }}</pre></details>
        </template>
      </aside>
    </div>

  </div>
</template>

<style scoped>
.document-polish{display:grid;gap:16px;margin:20px 0 28px}.document-polish select,.document-polish textarea{font:inherit;border:1px solid #dce4df;border-radius:10px;padding:12px;width:100%;box-sizing:border-box}.conversation-panel{background:white;width:min(720px,100%);padding:24px;overflow:auto;height:100dvh;box-sizing:border-box}.history-entry{display:grid;text-align:left;background:#f7faf8;border:1px solid #e1e8e4;padding:16px;border-radius:12px;width:100%;gap:10px;margin:12px 0;cursor:pointer}.history-message{border-bottom:1px solid #e1e8e4;padding:16px 0;white-space:pre-wrap;overflow-wrap:anywhere}.history-document{white-space:pre-wrap;overflow-wrap:anywhere;font:inherit}.history-entry small,.history-message small{color:#7a887f}
</style>

<style scoped>.stage-back-controls{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin:16px 0;font-size:15px;color:var(--ink-2)}.stage-back-controls select{font:inherit;padding:9px 12px;border:1px solid var(--line);border-radius:10px;background:white;max-width:100%}.stage-back-controls span{font-size:14px}</style>

<style scoped>
.document-overlay{align-items:stretch}.document-workspace{display:flex;background:#fff;width:min(960px,100%);height:100dvh;box-shadow:-12px 0 40px #182d2420;min-width:0}.document-workspace.with-editor{width:min(1400px,100%)}.section-editor{width:360px;flex-shrink:0;padding:28px;background:#f7f5fb;border-right:1px solid #e4deed;overflow:auto}.section-editor h2{font-size:22px;margin-bottom:26px}.section-editor label{display:block;margin-bottom:10px}.section-editor select{width:100%;font:inherit;padding:12px;border:1px solid #cfc4e2;border-radius:10px;background:white}.document-workspace :deep(.v2-prd){position:relative;display:flex;flex:1;width:0;min-width:0;height:100%;border:0}.document-workspace :deep(.v2-prd-close){display:inline-flex!important;align-items:center;justify-content:center;font-size:26px;width:40px;height:40px}.document-workspace :deep(.v2-prd-body){min-height:0;overflow:auto;flex:1}@media(max-width:800px){.document-workspace.with-editor{flex-direction:column}.section-editor{width:100%;max-height:45dvh;flex-shrink:0;border-right:0;border-bottom:1px solid #e4deed;padding:18px}.section-editor h2{margin-bottom:12px}.section-editor .assistant-input{min-height:80px;margin:12px 0}.document-workspace :deep(.v2-prd){width:100%;min-height:0}}
</style>
