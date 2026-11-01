<script setup>
import { computed } from 'vue'

const props = defineProps({
  turn: { type: Object, required: true },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['back', 'continue'])

const gate = computed(() => props.turn.gate || {})

const tone = computed(() => {
  if (gate.value.status === 'red' || gate.value.status === 'no-go') return 'bad'
  if (gate.value.status === 'yellow') return 'warn'
  return 'good'
})

const icon = computed(() => {
  if (tone.value === 'bad') return '!'
  if (tone.value === 'warn') return '!'
  return '✓'
})

const canContinue = computed(() => props.turn.canContinue !== false && tone.value !== 'bad')
</script>

<template>
  <div :class="['v2-gate', 'is-' + tone]">
    <div class="v2-gate-head">
      <span class="v2-gate-icon">{{ icon }}</span>
      <p class="v2-gate-message">{{ turn.message }}</p>
    </div>

    <ul v-if="turn.issues && turn.issues.length" class="v2-gate-issues">
      <li v-for="issue in turn.issues" :key="issue">{{ issue }}</li>
    </ul>

    <div class="v2-actions">
      <button
        v-if="canContinue"
        class="btn btn-primary"
        :disabled="disabled"
        @click="emit('continue')"
      >
        带着风险继续
      </button>
      <button
        v-if="gate.returnTo"
        class="btn btn-ghost"
        :disabled="disabled"
        @click="emit('back', gate.returnTo)"
      >
        回到第 {{ gate.returnTo }} 步
      </button>
    </div>
  </div>
</template>
