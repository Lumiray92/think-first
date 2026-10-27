<script setup>
import { computed } from 'vue'

import { SCORE_DIMS, discoverScore, scoreVerdict } from '@/lib/questions'
import { useSession } from '@/composables/useSession'

const { state, saveNow } = useSession()

const total = computed(() => discoverScore(state))
const verdict = computed(() => scoreVerdict(total.value))

function onSlider(key, event) {
  state.scores[key] = Number(event.target.value)
  saveNow()
}
</script>

<template>
  <div class="q">
    <div class="q-head">
      <span class="q-index">04</span>
      <div>
        <div class="q-title">五维评分是多少？</div>
      </div>
    </div>
    <div class="score-panel">
      <div v-for="dim in SCORE_DIMS" :key="dim.key" class="score-row" :data-dim="dim.key">
        <div class="score-name">{{ dim.name }}</div>
        <div class="score-slider">
          <input
            type="range"
            min="0"
            max="4"
            step="1"
            :value="state.scores[dim.key]"
            :data-score="dim.key"
            @input="onSlider(dim.key, $event)"
          />
          <div class="score-ticks">
            <span v-for="opt in dim.opts" :key="opt.label">{{ opt.label }}</span>
          </div>
        </div>
        <div class="score-value">{{ dim.opts[state.scores[dim.key]].v }}</div>
      </div>
      <div class="score-total" :class="verdict.cls">
        <div class="score-total-num">{{ total }}<span>/25</span></div>
        <div class="score-total-text">{{ verdict.text }}</div>
      </div>
    </div>
  </div>
</template>
