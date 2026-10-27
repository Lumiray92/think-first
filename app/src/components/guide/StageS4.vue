<script setup>
import { computed } from 'vue'

import FeatureList from './FeatureList.vue'
import QuestionBlock from './QuestionBlock.vue'
import { useSession } from '@/composables/useSession'

const { state, saveNow } = useSession()

const roi = computed({
  get: () => state.focusRoi || '',
  set: (value) => {
    state.focusRoi = value
    saveNow()
  },
})
</script>

<template>
  <QuestionBlock id="s4_1" index="01" />
  <QuestionBlock id="s4_2" index="02" />
  <FeatureList />
  <QuestionBlock id="s4_4" index="04" />
  <QuestionBlock id="s4_5" index="05" />
  <div class="q">
    <div class="q-head">
      <span class="q-index">06</span>
      <div>
        <div class="q-title">投入产出划算吗？</div>
      </div>
    </div>
    <textarea
      v-model="roi"
      class="q-input"
      rows="2"
      placeholder="例：开发约 10 小时；每天省 10 分钟，一年约 60 小时，大于 30 小时阈值。"
    ></textarea>
  </div>
</template>
