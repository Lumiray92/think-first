<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import BrandLogo from '@/components/BrandLogo.vue'
import DiaryModal from '@/components/DiaryModal.vue'
import NeuralCanvas from '@/components/NeuralCanvas.vue'
import { useToast } from '@/composables/useToast'
import { useSession } from '@/composables/useSession'

const router = useRouter()
const { toast } = useToast()
const { state } = useSession()

const isStepMode = ref(state.mode !== 'chat')
const diaryOpen = ref(false)

const startText = computed(() => (isStepMode.value ? '开始逐步思考' : '开始快速生成'))

function toggleMode() {
  isStepMode.value = !isStepMode.value
}

function start() {
  router.push(isStepMode.value ? '/guide' : '/chat')
}

function onDiarySaved() {
  toast('日记已保存到本地')
}
</script>

<template>
  <div class="home-view">
    <div class="aura" aria-hidden="true">
      <span class="blob b1"></span>
      <span class="blob b2"></span>
      <span class="blob b3"></span>
      <NeuralCanvas />
      <div class="grid"></div>
    </div>

    <nav class="topnav">
      <BrandLogo />
    </nav>

    <main class="stage-hero">
      <h1 class="headline rise">从想法到界定问题，<br />请留几分钟把需求推敲透彻再着手</h1>
      <p class="hero-sub rise">如何将模糊的想法，提炼为清晰的定义</p>
      <p class="hero-sub-2 rise">
        你会得到一份能直接粘给 Claude Code / Cursor / 同事的文档，而不是一堆散落的想法。
      </p>

      <div class="start-wrap rise">
        <div class="toggle-wrap" @click="toggleMode">
          <span class="toggle-label" :class="{ active: !isStepMode }">快速生成</span>
          <div class="toggle-track" :class="{ 'is-on': isStepMode }">
            <div class="toggle-thumb"></div>
          </div>
          <span class="toggle-label" :class="{ active: isStepMode }">逐步思考</span>
        </div>
        <button class="start" @click="start">{{ startText }}</button>
      </div>

      <div
        class="diary-trigger rise"
        role="button"
        tabindex="0"
        @click="diaryOpen = true"
        @keydown.enter="diaryOpen = true"
      >
        <span>📖 想不出做什么？先记一周烦恼日记</span>
      </div>
    </main>

    <DiaryModal :open="diaryOpen" @close="diaryOpen = false" @saved="onDiarySaved" />
  </div>
</template>
