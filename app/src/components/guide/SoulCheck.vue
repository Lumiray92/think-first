<script setup>
import { computed } from 'vue'

import { soulState } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const { state, saveNow } = useSession()

const rows = [
  { key: 'whoLight', label: '用户：' },
  { key: 'painLight', label: '痛点：' },
  { key: 'whyYouLight', label: '为什么选你：' },
]

const lights = [
  { value: 'green', cls: 'light-g', text: '🟢 清晰' },
  { value: 'yellow', cls: 'light-y', text: '🟡 说不清' },
  { value: 'red', cls: 'light-r', text: '🔴 答不出' },
]

const status = computed(() => soulState(state))

const conclusion = computed(() => {
  if (status.value === 'ok') return { cls: 'ok', lead: '可以开始开发。', rest: '三个问题全绿。' }
  if (status.value === 'warn') return { cls: 'warn', lead: '建议补强。', rest: '有黄灯，可以先做最小实验再补齐。' }
  if (status.value === 'stop') return { cls: 'stop', lead: '先停下来。', rest: '有红灯，回到对应阶段想清楚再往下走。' }
  return { cls: '', lead: '把三个红绿灯都点亮', rest: '，这里会告诉你现在能不能开始动手。' }
})

function toggle(key, value) {
  state.selfcheck[key] = state.selfcheck[key] === value ? null : value
  saveNow()
}
</script>

<template>
  <div class="q">
    <div class="q-head">
      <span class="q-index">07</span>
      <div>
        <div class="q-title">红黄绿结论</div>
      </div>
    </div>
    <div
      v-for="row in rows"
      :key="row.key"
      class="traffic"
      style="margin-top: 8px"
    >
      <span class="traffic-label">{{ row.label }}</span>
      <button
        v-for="light in lights"
        :key="light.value"
        class="light"
        :class="[light.cls, { 'is-on': state.selfcheck[row.key] === light.value }]"
        @click="toggle(row.key, light.value)"
      >
        {{ light.text }}
      </button>
    </div>
    <div class="soul-conclusion" :class="conclusion.cls">
      <b>{{ conclusion.lead }}</b>{{ conclusion.rest }}
    </div>
  </div>
</template>
