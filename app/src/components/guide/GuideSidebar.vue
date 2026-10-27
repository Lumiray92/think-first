<script setup>
import { computed, nextTick, ref } from 'vue'

import BrandLogo from '@/components/BrandLogo.vue'
import { GUIDE_CASES, GUIDE_CASE_SCENES } from '@/lib/guideCases'

defineProps({
  collapsed: { type: Boolean, default: false },
})

const emit = defineEmits(['toggle'])

const bodyRef = ref(null)
const activeFilter = ref('all')
const activeCaseId = ref('')
const openStages = ref([])

const filteredCases = computed(() => {
  if (activeFilter.value === 'all') return GUIDE_CASES
  return GUIDE_CASES.filter((item) => item.sceneKey === activeFilter.value)
})

const activeCase = computed(
  () => GUIDE_CASES.find((item) => item.id === activeCaseId.value) || null,
)

const allOpen = computed(
  () =>
    !!activeCase.value &&
    openStages.value.length === activeCase.value.stages.length,
)

const LIGHTS = {
  green: { emoji: '🟢', label: '清晰', cls: 'is-green' },
  yellow: { emoji: '🟡', label: '说不清', cls: 'is-yellow' },
  red: { emoji: '🔴', label: '答不出', cls: 'is-red' },
}

function lightInfo(value) {
  return LIGHTS[value] || { emoji: '⚪', label: '待判断', cls: 'is-none' }
}

function scoreTone(score) {
  if (score >= 20) return 'is-good'
  if (score >= 15) return 'is-mid'
  if (score >= 10) return 'is-warn'
  return 'is-bad'
}

function scrollTop() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = 0
  })
}

function openCase(id) {
  const item = GUIDE_CASES.find((entry) => entry.id === id)
  if (!item) return
  activeCaseId.value = id
  openStages.value = item.stages.map((stage) => stage.no)
  scrollTop()
}

function backToList() {
  activeCaseId.value = ''
  openStages.value = []
  scrollTop()
}

function toggleStage(no) {
  if (openStages.value.includes(no)) {
    openStages.value = openStages.value.filter((item) => item !== no)
  } else {
    openStages.value = [...openStages.value, no]
  }
}

function isStageOpen(no) {
  return openStages.value.includes(no)
}

function toggleAll() {
  if (!activeCase.value) return
  openStages.value = allOpen.value ? [] : activeCase.value.stages.map((stage) => stage.no)
}

function pickFilter(key) {
  activeFilter.value = key
  scrollTop()
}
</script>

<template>
  <aside class="guide-sidebar" :class="{ 'is-collapsed': collapsed }" aria-label="详细案例库">
    <div class="sidebar-header guide-sidebar-header">
      <BrandLogo />
      <button class="chat-toggle-btn" aria-label="收起案例库" @click="emit('toggle')">◀</button>
    </div>

    <div ref="bodyRef" class="guide-sidebar-body">
      <template v-if="!activeCase">
        <div class="case-library-head">
          <div>
            <h2>详细案例库</h2>
            <p>4 个完整示例 · 按 9 个阶段对照</p>
          </div>
          <span class="case-count">{{ filteredCases.length }}</span>
        </div>

        <div class="case-filters" role="tablist" aria-label="按场景筛选案例">
          <button
            v-for="scene in GUIDE_CASE_SCENES"
            :key="scene.key"
            class="case-filter"
            :class="{ 'is-on': activeFilter === scene.key }"
            @click="pickFilter(scene.key)"
          >
            {{ scene.label }}
          </button>
        </div>

        <div v-if="!filteredCases.length" class="case-empty">这个场景还没有案例。</div>

        <article
          v-for="item in filteredCases"
          :key="item.id"
          class="case-card"
          role="button"
          tabindex="0"
          @click="openCase(item.id)"
          @keydown.enter="openCase(item.id)"
          @keydown.space.prevent="openCase(item.id)"
        >
          <div class="case-card-top">
            <span class="case-scene">{{ item.scene }}</span>
            <span class="case-score" :class="scoreTone(item.score)">{{ item.score }} / 25</span>
          </div>
          <h3 class="case-card-title">{{ item.title }}</h3>
          <p class="case-card-summary">{{ item.summary }}</p>
          <div class="case-tags">
            <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
          </div>
          <div class="case-card-foot">
            <span>{{ item.scoreVerdict }}</span>
            <span class="case-card-open">查看详情 →</span>
          </div>
        </article>

        <p class="case-library-note">
          案例只用于对照参考，不会覆盖你当前填写的内容。
        </p>
      </template>

      <template v-else>
        <button class="case-back" @click="backToList">← 返回案例库</button>

        <div class="case-detail-head">
          <div class="case-detail-badges">
            <span class="case-scene">{{ activeCase.scene }}</span>
            <span class="case-score" :class="scoreTone(activeCase.score)">
              {{ activeCase.score }} / 25 · {{ activeCase.scoreVerdict }}
            </span>
          </div>
          <h2>{{ activeCase.title }}</h2>
          <p>{{ activeCase.subtitle }}</p>
        </div>

        <div class="case-metrics">
          <div v-for="metric in activeCase.metrics" :key="metric.label" class="case-metric">
            <span>{{ metric.label }}</span>
            <b>{{ metric.value }}</b>
          </div>
        </div>

        <div class="case-callout">
          <span>一句话问题</span>
          <p>{{ activeCase.problem }}</p>
        </div>

        <div class="case-lights">
          <span class="case-light" :class="lightInfo(activeCase.lights.who).cls">
            用户 {{ lightInfo(activeCase.lights.who).emoji }} {{ lightInfo(activeCase.lights.who).label }}
          </span>
          <span class="case-light" :class="lightInfo(activeCase.lights.pain).cls">
            痛点 {{ lightInfo(activeCase.lights.pain).emoji }} {{ lightInfo(activeCase.lights.pain).label }}
          </span>
          <span class="case-light" :class="lightInfo(activeCase.lights.why).cls">
            为什么选你 {{ lightInfo(activeCase.lights.why).emoji }} {{ lightInfo(activeCase.lights.why).label }}
          </span>
        </div>

        <div class="case-stage-toolbar">
          <span>9 个阶段</span>
          <button @click="toggleAll">{{ allOpen ? '全部收起' : '全部展开' }}</button>
        </div>

        <section v-for="stage in activeCase.stages" :key="stage.no" class="case-stage">
          <button
            class="case-stage-head"
            :aria-expanded="isStageOpen(stage.no)"
            @click="toggleStage(stage.no)"
          >
            <span class="case-stage-no">{{ stage.no }}</span>
            <span class="case-stage-name">{{ stage.name }}</span>
            <span class="case-stage-chevron" :class="{ 'is-open': isStageOpen(stage.no) }">⌄</span>
          </button>
          <div v-show="isStageOpen(stage.no)" class="case-stage-body">
            <div v-for="(item, index) in stage.items" :key="index" class="case-qa">
              <div class="case-q">{{ item.q }}</div>
              <div class="case-a">{{ item.a }}</div>
            </div>
          </div>
        </section>

        <div class="case-takeaway">
          <span>关键取舍</span>
          <p>{{ activeCase.takeaway }}</p>
        </div>

        <p class="case-foot">案例是完整示例，不是唯一答案；先照着走一遍，再写自己的。</p>
      </template>
    </div>
  </aside>
</template>
