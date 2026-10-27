<script setup>
import { computed } from 'vue'

import { Q } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const props = defineProps({
  id: { type: String, required: true },
  index: { type: String, default: '' },
})

const { state, saveNow } = useSession()

const q = computed(() => Q[props.id])
const answer = computed({
  get: () => state.answers[props.id] || '',
  set: (value) => {
    state.answers[props.id] = value
    saveNow()
  },
})
</script>

<template>
  <div v-if="q" class="q">
    <div class="q-head">
      <span class="q-index">{{ index }}</span>
      <div>
        <div class="q-title">{{ q.title }}</div>
      </div>
    </div>
    <textarea v-model="answer" class="q-input" rows="3" :placeholder="q.ph"></textarea>
  </div>
</template>
