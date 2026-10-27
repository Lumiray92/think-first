<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'

import BrandLogo from '@/components/BrandLogo.vue'
import { STAGES, stageDone } from '@/lib/questions'
import { buildAiPrompt, copyText, downloadMarkdown } from '@/lib/doc'
import { useSession } from '@/composables/useSession'
import { useToast } from '@/composables/useToast'

const { state } = useSession()
const { toast } = useToast()

const promptOpen = ref(false)
const promptText = ref('')
const copyTip = ref('粘贴到 Claude Code / Cursor / ChatGPT 都可以')

const isChatMode = computed(() => state.mode === 'chat')
const doneCount = computed(() => STAGES.filter((stage) => stageDone(state, stage)).length)
const missingStages = computed(() => STAGES.filter((stage) => !stageDone(state, stage)))
const hasChat = computed(() => !!state.chatAnswers.chat_q1)
const isEmpty = computed(() =>
  isChatMode.value ? !hasChat.value : doneCount.value === 0,
)
const showTodo = computed(
  () => !isChatMode.value && doneCount.value > 0 && missingStages.value.length > 0,
)
const railDoneCount = computed(() => (isChatMode.value ? 0 : doneCount.value))

const title = computed(() => {
  if (isEmpty.value) return '文档还是空的'
  if (isChatMode.value) return '快速判断已完成'
  return '终于完成了'
})
const badgeChar = computed(() => (isEmpty.value ? '—' : '✓'))
const leadBold = computed(() => {
  if (isEmpty.value) return ''
  if (isChatMode.value) return '你已经完成了 60 秒快速判断。'
  return doneCount.value + ' / ' + STAGES.length + ' 个阶段已完成。'
})
const leadSub = computed(() => {
  if (isEmpty.value) return '你还没有填任何内容。回到首页重新开始吧。'
  if (isChatMode.value) {
    return '想拿到更完整的需求文档，可以回到首页用分阶段模式补充用户、风险和 MVP 范围。'
  }
  return '现在你手里这份文档，比三周后的返工便宜得多。'
})

function openPrompt() {
  promptText.value = buildAiPrompt(state)
  copyTip.value = '粘贴到 Claude Code / Cursor / ChatGPT 都可以'
  promptOpen.value = true
}

function closePrompt() {
  promptOpen.value = false
  copyTip.value = '粘贴到 Claude Code / Cursor / ChatGPT 都可以'
}

function copyPrompt() {
  copyText(buildAiPrompt(state)).then((ok) => {
    copyTip.value = ok ? '✓ 已复制。现在把它粘给你的编码 AI。' : '复制失败，请手动全选上面的文本复制'
  })
}

function exportMd() {
  const name = downloadMarkdown(state)
  toast('已导出 ' + name + '.md')
}

function onKeydown(event) {
  if (event.key === 'Escape') closePrompt()
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="done-view">
    <header class="topbar">
      <BrandLogo />
      <div class="topbar-mid">
        <div class="rail">
          <template v-for="(stage, i) in STAGES" :key="stage.key">
            <span
              v-if="i > 0"
              class="rail-link"
              :class="{ 'is-flow': railDoneCount >= i }"
            ></span>
            <span
              class="rail-item"
              :class="{ 'is-done': !isChatMode && stageDone(state, stage) }"
              :title="stage.name"
            >
              <span class="rail-dot">{{ i + 1 }}</span>
              <span class="rail-name">{{ stage.name }}</span>
            </span>
          </template>
        </div>
      </div>
      <div class="save-state"><span class="live-dot"></span> 已保存到本地</div>
    </header>

    <main class="done-wrap">
      <section class="celebrate">
        <div class="celebrate-badge" :class="{ 'is-empty': isEmpty }">{{ badgeChar }}</div>
        <h1>{{ title }}</h1>
        <p class="lead">
          <template v-if="leadBold"><b>{{ leadBold }}</b><br></template>
          {{ leadSub }}
        </p>
      </section>

      <section v-if="showTodo" class="todo">
        <span class="todo-icon">⚠</span>
        <div>
          <b>还有 {{ missingStages.length }} 个阶段没填完</b>
          <span>
            <span v-for="stage in missingStages" :key="stage.key" class="empty-chip">
              {{ stage.name }}
            </span>
            这些内容在文档里会显示成「<span class="empty-chip">待补</span>」—— 不影响导出，但补上之后这份文档才真的能用。
          </span>
          <div style="margin-top: 12px">
            <RouterLink class="btn btn-ghost" to="/">回到首页</RouterLink>
          </div>
        </div>
      </section>

      <section class="actions">
        <button class="action primary" @click="openPrompt">
          <span>复制为 AI Prompt</span>
          <span class="sub">直接粘给你的编码 AI</span>
        </button>
        <button class="action" @click="exportMd">
          <span>导出 Markdown</span>
          <span class="sub">存成 .md 文件留着</span>
        </button>
      </section>

      <p class="done-foot">
        数据只存在你的浏览器里 ·
        <RouterLink class="btn-quiet" to="/" style="padding: 0; font-size: 13px">
          回到首页
        </RouterLink>
      </p>
    </main>

    <div v-if="promptOpen" class="modal-mask" @click.self="closePrompt">
      <div class="modal">
        <div class="modal-head">
          <div>
            <h3>把这段直接粘给你的编码 AI</h3>
            <p>它已经带上了硬性约束，AI 不会自作主张给你加登录、加后台。</p>
          </div>
          <button class="modal-close" @click="closePrompt">×</button>
        </div>
        <div class="modal-body"><pre>{{ promptText }}</pre></div>
        <div class="modal-foot">
          <span class="tip">{{ copyTip }}</span>
          <button class="btn btn-primary" @click="copyPrompt">复制全文</button>
        </div>
      </div>
    </div>
  </div>
</template>
