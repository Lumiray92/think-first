<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import DocBubble from '@/components/guide/DocBubble.vue'
import GuideSidebar from '@/components/guide/GuideSidebar.vue'
import StageDefault from '@/components/guide/StageDefault.vue'
import StageRail from '@/components/guide/StageRail.vue'
import StageS0 from '@/components/guide/StageS0.vue'
import StageS4 from '@/components/guide/StageS4.vue'
import StageS5 from '@/components/guide/StageS5.vue'
import StageS6 from '@/components/guide/StageS6.vue'
import { STAGES } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const router = useRouter()
const { currentStage, savedAt, goStage, setMode } = useSession()

setMode('guide')

const sidebarCollapsed = ref(false)

const stage = computed(() => STAGES[currentStage.value])
const isLast = computed(() => currentStage.value === STAGES.length - 1)

const liquidHeight = computed(() => {
  const pct = 16 + (currentStage.value / (STAGES.length - 1)) * 84
  return pct.toFixed(1) + '%'
})

const stageComponent = computed(() => {
  switch (stage.value.special) {
    case 's0':
      return StageS0
    case 's4':
      return StageS4
    case 's5':
      return StageS5
    case 's6':
      return StageS6
    default:
      return StageDefault
  }
})

const stageProps = computed(() =>
  stageComponent.value === StageDefault ? { ids: stage.value.ids || [] } : {},
)

function pad(n) {
  return String(n).padStart(2, '0')
}

function prev() {
  goStage(currentStage.value - 1)
}

function next() {
  if (isLast.value) router.push('/done')
  else goStage(currentStage.value + 1)
}

function goDone() {
  router.push('/done')
}

onMounted(() => {
  if (window.innerWidth < 820) sidebarCollapsed.value = true
})
</script>

<template>
  <div class="guide-view" :class="{ 'is-sidebar-collapsed': sidebarCollapsed }">
    <GuideSidebar :collapsed="sidebarCollapsed" @toggle="sidebarCollapsed = !sidebarCollapsed" />
    <div
      v-if="!sidebarCollapsed"
      class="guide-sidebar-scrim"
      @click="sidebarCollapsed = true"
    ></div>

    <div class="page-liquid-bg" :style="{ height: liquidHeight }"></div>

    <header class="topbar">
      <button
        v-show="sidebarCollapsed"
        class="chat-toggle-btn guide-topbar-toggle"
        aria-label="展开案例库"
        @click="sidebarCollapsed = false"
      >
        ▶
      </button>
      <div class="topbar-mid">
        <StageRail />
      </div>
      <div class="save-state"><span class="live-dot"></span> 已保存 · {{ savedAt }}</div>
    </header>

    <main class="workspace">
      <div :key="currentStage" class="stage-card anim-float">
        <div class="stageblock-head">
          <span class="stageblock-no">{{ pad(currentStage + 1) }} / {{ pad(STAGES.length) }}</span>
          <span class="stageblock-name">{{ stage.name }}</span>
        </div>
        <h2 class="stage-title">{{ stage.title }}</h2>
        <div class="stage-body">
          <component :is="stageComponent" v-bind="stageProps" />
        </div>
        <div class="stage-nav">
          <button class="btn btn-ghost" :disabled="currentStage === 0" @click="prev">上一阶段</button>
          <span class="stage-nav-hint">也可点顶部导航直接跳转</span>
          <button v-if="isLast" class="btn btn-primary" @click="goDone">看结果</button>
          <button v-else class="btn btn-primary" @click="next">下一阶段</button>
        </div>
      </div>
    </main>

    <div class="bottom-bar">
      <button class="btn btn-primary btn-lg" @click="goDone">填完了，看结果</button>
    </div>

    <DocBubble />
  </div>
</template>
