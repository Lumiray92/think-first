<script setup>
import { computed } from 'vue'

import { featureVerdict } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const props = defineProps({
  feature: { type: Object, required: true },
})
const emit = defineEmits(['remove'])

const { saveNow } = useSession()

const verdict = computed(() => featureVerdict(props.feature))
const showWhy = computed(() => verdict.value === 'P1' || verdict.value === 'P2')
const verdictClass = computed(() => (verdict.value ? 'v-' + verdict.value.toLowerCase() : 'v-none'))

const questions = [
  { key: 'q1', label: 'Q1 没有它，产品还能用吗？', left: '能', right: '不能', invert: true },
  { key: 'q2', label: 'Q2 它能验证你的核心假设吗？', left: '能', right: '不能', invert: false },
  { key: 'q3', label: 'Q3 第一次就需要它吗？', left: '是', right: '否', invert: false },
]

function leftValue(q) {
  return q.invert ? 0 : 1
}

function rightValue(q) {
  return q.invert ? 1 : 0
}

function toggle(q, value) {
  props.feature[q.key] = props.feature[q.key] === value ? null : value
  saveNow()
}

function onName(event) {
  props.feature.name = event.target.value
  saveNow()
}

function onWhy(event) {
  props.feature.whyNot = event.target.value
  saveNow()
}
</script>

<template>
  <div class="feature-row" :data-fid="feature.id">
    <div class="feature-top">
      <input
        class="feature-name"
        :data-fname="feature.id"
        placeholder="例：快速输入、本地保存、每日提醒"
        :value="feature.name"
        @input="onName"
      />
      <span class="feature-verdict" :class="verdictClass" :data-fverdict="feature.id">
        {{ verdict || '—' }}
      </span>
      <button class="feature-remove" title="删除" @click="emit('remove', feature.id)">×</button>
    </div>
    <div class="feature-qs">
      <div v-for="q in questions" :key="q.key" class="fq">
        <span class="fq-label">{{ q.label }}</span>
        <div class="seg">
          <button
            :class="{ 'is-on': feature[q.key] === leftValue(q) }"
            @click="toggle(q, leftValue(q))"
          >
            {{ q.left }}
          </button>
          <button
            :class="{ 'is-on': feature[q.key] === rightValue(q) }"
            @click="toggle(q, rightValue(q))"
          >
            {{ q.right }}
          </button>
        </div>
      </div>
    </div>
    <div v-if="showWhy" class="why-not">
      <label>为什么现在不做</label>
      <input
        :value="feature.whyNot || ''"
        placeholder="例：它们不影响验证，先不做。"
        @input="onWhy"
      />
    </div>
  </div>
</template>
