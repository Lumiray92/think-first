<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'

import BrandLogo from '@/components/BrandLogo.vue'
import DiaryModal from '@/components/DiaryModal.vue'
import DocBubble from '@/components/guide/DocBubble.vue'
import {
  ANSWER_IDS,
  CHAT_SCRIPTS,
  DEFAULT_CASES,
  TRAFFIC_REPLIES,
  chatKeyForStep,
  readSavedCases,
  saveCase,
} from '@/lib/chat'
import { useSession } from '@/composables/useSession'
import { useToast } from '@/composables/useToast'

const { state, saveNow, setMode } = useSession()
const { toast } = useToast()

setMode('chat')

const currentKey = ref('')
const inputText = ref('')
const inputRef = ref(null)
const historyRef = ref(null)
const sidebarCollapsed = ref(false)
const activeTab = ref('records')
const viewingMode = ref('current')
const viewingHistory = ref([])
const diaryOpen = ref(false)
const savedCases = ref(readSavedCases())

let nextTimer = null

const displayHistory = computed(() =>
  viewingMode.value === 'current' ? state.chatHistory : viewingHistory.value,
)
const userCount = computed(() => state.chatHistory.filter((m) => m.role === 'user').length)
const allCases = computed(() => [
  ...DEFAULT_CASES,
  ...savedCases.value.map((item, index) => ({ ...item, id: 'saved_' + index })),
])
const liquidHeight = computed(() => {
  if (viewingMode.value === 'case') {
    const agentCount = viewingHistory.value.filter((msg) => msg.role === 'agent').length
    return (10 + (Math.min(agentCount, 8) / 8) * 90).toFixed(1) + '%'
  }
  return (10 + (Math.min(state.chatStep, 8) / 8) * 90).toFixed(1) + '%'
})
const canType = computed(
  () =>
    viewingMode.value === 'current' &&
    !!currentKey.value &&
    currentKey.value !== 'check1' &&
    currentKey.value !== 'end',
)
const inputPlaceholder = computed(() => {
  if (currentKey.value === 'check1') return '请先选择上面的清晰度'
  if (currentKey.value === 'end') return '本次对话已完成'
  return '输入你的回答...'
})
const showTraffic = computed(
  () =>
    viewingMode.value === 'current' &&
    currentKey.value === 'check1' &&
    !state.chatLights.fast,
)
const showChoiceActions = computed(
  () =>
    viewingMode.value === 'current' &&
    currentKey.value === 'check1' &&
    !!state.chatLights.fast,
)
const showResultCard = computed(
  () => viewingMode.value === 'current' && currentKey.value === 'end',
)

const choiceActions = computed(() => {
  if (!showChoiceActions.value) return []
  if (state.chatLights.fast === 'green') {
    return [
      { label: '进入完整版检查', primary: true, run: startFullCheck },
      { label: '跳过，直接看结果', primary: false, run: endChat },
    ]
  }
  if (state.chatLights.fast === 'yellow') {
    return [
      { label: '进入完整版', primary: true, run: startFullCheck },
      { label: '重新快速判断', primary: false, run: resetChat },
    ]
  }
  return [
    { label: '重新梳理一遍', primary: true, run: resetChat },
    { label: '去记烦恼日记', primary: false, run: openDiary },
  ]
})

function escapeHtml(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function formatMessage(text) {
  return escapeHtml(text).replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

function scrollToBottom() {
  const el = historyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function startChatScript(key) {
  const script = CHAT_SCRIPTS[key]
  if (!script) return
  currentKey.value = key
  const last = state.chatHistory[state.chatHistory.length - 1]
  if (!(last && last.role === 'agent' && last.text === script.text)) {
    state.chatHistory.push({ role: 'agent', text: script.text, type: script.type })
    saveNow()
  }
  nextTick(scrollToBottom)
}

function handleUserSend() {
  const text = inputText.value.trim()
  if (!text || viewingMode.value !== 'current') return
  const key = currentKey.value
  if (!key || key === 'check1' || key === 'end') return

  const answerId = ANSWER_IDS[key]
  if (answerId) state.chatAnswers[answerId] = text
  state.chatHistory.push({ role: 'user', text })
  inputText.value = ''
  nextTick(resetInputHeight)
  state.chatStep++
  saveNow()

  const script = CHAT_SCRIPTS[key]
  if (script && script.next) {
    currentKey.value = ''
    clearTimeout(nextTimer)
    nextTimer = setTimeout(() => {
      nextTimer = null
      startChatScript(script.next)
    }, 600)
  }
  nextTick(scrollToBottom)
}

function handleTraffic(light) {
  if (state.chatLights.fast) return
  state.chatLights.fast = light
  const replies = TRAFFIC_REPLIES[light] || []
  replies.forEach((text) => state.chatHistory.push({ role: 'agent', text }))
  saveNow()
  nextTick(scrollToBottom)
}

function startFullCheck() {
  clearTimeout(nextTimer)
  nextTimer = null
  state.chatStep = 4
  saveNow()
  startChatScript('full1')
}

function endChat() {
  clearTimeout(nextTimer)
  nextTimer = null
  state.chatStep = 8
  saveNow()
  startChatScript('end')
}

function resetChat() {
  clearTimeout(nextTimer)
  nextTimer = null
  state.chatAnswers = {}
  state.chatHistory = []
  state.chatStep = 0
  state.chatLights = {}
  viewingMode.value = 'current'
  viewingHistory.value = []
  inputText.value = ''
  currentKey.value = ''
  saveNow()
  startChatScript('q1')
  nextTick(() => {
    resetInputHeight()
    scrollToBottom()
  })
}

function openDiary() {
  diaryOpen.value = true
}

function onDiarySaved() {
  toast('日记已保存到本地')
}

function setTab(tab) {
  activeTab.value = tab
}

function loadCase(id) {
  if (id === 'current') {
    viewingMode.value = 'current'
  } else if (id.startsWith('saved_')) {
    const index = Number(id.slice('saved_'.length))
    const item = savedCases.value[index]
    if (!item || !item.history || !item.history.length) return
    viewingHistory.value = item.history
    viewingMode.value = 'case'
  } else {
    const item =
      DEFAULT_CASES.find((entry) => entry.id === id) ||
      allCases.value.find((entry) => entry.id === id)
    if (!item || !item.history || !item.history.length) return
    viewingHistory.value = item.history
    viewingMode.value = 'case'
  }
  if (window.innerWidth < 820) sidebarCollapsed.value = true
  nextTick(scrollToBottom)
}

function saveCurrentCase() {
  if (!state.chatHistory.length) return
  const title = window.prompt(
    '给这段思考记录起个名字：',
    '我的思考 ' + new Date().toLocaleTimeString(),
  )
  if (!title) return
  savedCases.value = saveCase(title, state.chatHistory)
  activeTab.value = 'records'
  toast('已添加到案例库')
}

function onInputKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    handleUserSend()
  }
}

function autoResize(event) {
  const el = event.target
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}

function resetInputHeight() {
  if (inputRef.value) inputRef.value.style.height = 'auto'
}

watch(
  () => displayHistory.value.length,
  () => nextTick(scrollToBottom),
)
watch(currentKey, () => nextTick(scrollToBottom))

onMounted(() => {
  if (window.innerWidth < 820) sidebarCollapsed.value = true
  if (!state.chatHistory.length) {
    startChatScript('q1')
  } else {
    currentKey.value = chatKeyForStep(state.chatStep)
    const last = state.chatHistory[state.chatHistory.length - 1]
    if (last && last.role === 'user') startChatScript(currentKey.value)
  }
  nextTick(scrollToBottom)
})

onBeforeUnmount(() => {
  clearTimeout(nextTimer)
})
</script>

<template>
  <div class="chat-view">
    <div class="chat-liquid-bg" :style="{ height: liquidHeight }"></div>

    <aside class="chat-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <BrandLogo />
        <button class="chat-toggle-btn" @click="sidebarCollapsed = true">◀</button>
      </div>
      <div class="sidebar-tabs">
        <div
          class="sidebar-tab"
          :class="{ active: activeTab === 'records' }"
          @click="setTab('records')"
        >
          思考记录
        </div>
        <div
          class="sidebar-tab"
          :class="{ active: activeTab === 'cases' }"
          @click="setTab('cases')"
        >
          案例库
        </div>
      </div>
      <div class="sidebar-content">
        <template v-if="activeTab === 'records'">
          <div
            v-if="!state.chatHistory.length"
            class="record-desc"
            style="text-align: center; margin-top: 20px"
          >
            还没有记录，在右侧开始对话吧。
          </div>
          <template v-else>
            <div class="record-item" @click="loadCase('current')">
              <div class="record-title">当前对话 (点击查看)</div>
              <div class="record-desc">共 {{ userCount }} 次回答</div>
            </div>
            <div
              v-for="(item, index) in savedCases"
              :key="'saved-' + index"
              class="record-item"
              @click="loadCase('saved_' + index)"
            >
              <div class="record-title">{{ item.title }}</div>
              <div class="record-desc">已保存的思考</div>
            </div>
          </template>
        </template>
        <template v-else>
          <div
            v-for="item in allCases"
            :key="item.id"
            class="record-item"
            @click="loadCase(item.id)"
          >
            <div class="record-title">{{ item.title }}</div>
            <div class="record-desc">点击回放分析过程</div>
          </div>
          <button
            v-if="userCount > 0"
            class="btn btn-primary"
            style="margin-top: 16px"
            @click="saveCurrentCase"
          >
            + 保存当前对话为案例
          </button>
        </template>
      </div>
    </aside>

    <div class="chat-main">
      <div class="chat-header">
        <div style="display: flex; align-items: center; gap: 10px">
          <button
            v-show="sidebarCollapsed"
            class="chat-toggle-btn"
            @click="sidebarCollapsed = false"
          >
            ▶
          </button>
          <RouterLink class="btn btn-ghost" to="/" style="padding: 6px 12px; font-size: 13px">
            ← 返回首页
          </RouterLink>
          <button
            v-if="viewingMode !== 'current'"
            class="btn btn-ghost"
            style="padding: 6px 12px; font-size: 13px"
            @click="loadCase('current')"
          >
            返回当前对话
          </button>
        </div>
        <div class="save-state"><span class="live-dot"></span> 自动保存</div>
      </div>

      <div ref="historyRef" class="chat-history">
        <div
          v-for="(msg, index) in displayHistory"
          :key="index"
          class="chat-msg"
          :class="msg.role"
        >
          <div class="chat-bubble" v-html="formatMessage(msg.text)"></div>
        </div>

        <div v-if="showTraffic" class="traffic-buttons">
          <button class="traffic-btn green" @click="handleTraffic('green')">🟢 很清晰</button>
          <button class="traffic-btn yellow" @click="handleTraffic('yellow')">🟡 说不清</button>
          <button class="traffic-btn red" @click="handleTraffic('red')">🔴 答不出</button>
        </div>

        <div v-if="showChoiceActions" class="chat-choice-actions">
          <button
            v-for="item in choiceActions"
            :key="item.label"
            class="btn"
            :class="item.primary ? 'btn-primary' : 'btn-ghost'"
            @click="item.run"
          >
            {{ item.label }}
          </button>
        </div>

        <div v-if="showResultCard" class="chat-msg agent">
          <div class="chat-result-card">
            <h4>所有问题已问完</h4>
            <p>你的需求文档已生成完毕，可以去查看完整结果了。</p>
            <RouterLink class="btn btn-primary" to="/done">查看最终文档 →</RouterLink>
          </div>
        </div>
      </div>

      <div v-if="viewingMode === 'current'" class="chat-input-area">
        <textarea
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          rows="1"
          :disabled="!canType"
          :placeholder="inputPlaceholder"
          @keydown="onInputKeydown"
          @input="autoResize"
        ></textarea>
        <button class="btn-send" :disabled="!canType" @click="handleUserSend">
          <svg viewBox="0 0 24 24">
            <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" />
          </svg>
        </button>
      </div>
    </div>

    <DocBubble />
    <DiaryModal :open="diaryOpen" @close="diaryOpen = false" @saved="onDiarySaved" />
  </div>
</template>
