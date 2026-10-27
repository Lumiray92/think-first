<script setup>
import { STAGES, stageDone } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const { state, currentStage, goStage } = useSession()
</script>

<template>
  <div class="rail">
    <template v-for="(stage, i) in STAGES" :key="stage.key">
      <span v-if="i > 0" class="rail-link" :class="{ 'is-flow': currentStage >= i }"></span>
      <button
        class="rail-item"
        :class="{ 'is-done': stageDone(state, stage), 'is-current': i === currentStage }"
        :title="stage.name"
        :aria-label="stage.name"
        @click="goStage(i)"
      >
        <span class="rail-dot">{{ i + 1 }}</span>
        <span class="rail-name">{{ stage.name }}</span>
      </button>
    </template>
  </div>
</template>
