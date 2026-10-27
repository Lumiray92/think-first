<script setup>
import QuestionBlock from './QuestionBlock.vue'

defineProps({
  ids: { type: Array, default: () => [] },
})

function pad(n) {
  return String(n).padStart(2, '0')
}
</script>

<template>
  <QuestionBlock
    v-for="(id, i) in ids"
    :key="id"
    :id="id"
    :index="pad(i + 1)"
  />
</template>
