<script setup>
import { computed } from 'vue'

const props = defineProps({
  stages: { type: Array, default: () => [] },
  stage: { type: Number, default: 1 },
  group: { type: Number, default: 1 },
  mode: { type: String, default: 'quick' },
  busy: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const current = computed(() => Number(props.stage) || 1)

function statusOf(no) {
  if (no < current.value) return 'done'
  if (no === current.value) return 'current'
  return 'todo'
}

function onSelect(no) {
  if (props.busy || no >= current.value) return
  emit('select', no)
}
</script>

<template>
  <aside class="v2-rail">
    <div class="v2-rail-head">
      <span class="v2-kicker">{{ mode === 'quick' ? '快速模式' : '完整模式' }}</span>
      <strong>{{ current }} / {{ stages.length }}</strong>
    </div>
    <div v-if="mode === 'quick'" class="v2-rail-group">第 {{ group }} 组 · 共 5 组</div>

    <ol class="v2-rail-list">
      <li
        v-for="item in stages"
        :key="item.no"
        :class="[
          'v2-rail-item',
          'is-' + statusOf(item.no),
          { 'is-clickable': item.no < current, 'has-gate': item.gate },
        ]"
        @click="onSelect(item.no)"
      >
        <span class="v2-rail-dot">{{ item.no }}</span>
        <span class="v2-rail-text">
          <span class="v2-rail-name">{{ item.name }}</span>
          <span class="v2-rail-agent">{{ item.agent }}</span>
        </span>
        <span v-if="item.gate" class="v2-rail-gate" title="闸门">闸</span>
      </li>
    </ol>

    <p class="v2-rail-note">点已完成的步骤可以回去修改，后面的内容会自动清空重来。</p>
  </aside>
</template>
