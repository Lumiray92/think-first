<script setup>
import FeatureRow from './FeatureRow.vue'
import { useSession } from '@/composables/useSession'

const { state, saveNow } = useSession()

function addFeature() {
  state.features.push({
    id: 'f' + Date.now() + Math.random().toString(36).slice(2, 6),
    name: '',
    q1: null,
    q2: null,
    q3: null,
    whyNot: '',
  })
  saveNow()
}

function removeFeature(id) {
  state.features = state.features.filter((f) => f.id !== id)
  saveNow()
}
</script>

<template>
  <div class="q">
    <div class="q-head">
      <span class="q-index">03</span>
      <div>
        <div class="q-title">第一版只做哪 3～5 个功能？为什么？</div>
      </div>
    </div>
    <div class="feature-list">
      <FeatureRow
        v-for="feature in state.features"
        :key="feature.id"
        :feature="feature"
        @remove="removeFeature"
      />
    </div>
    <button class="btn btn-ghost" style="margin-top: 14px; width: 100%; border-style: dashed" @click="addFeature">
      + 添加一个功能
    </button>
    <div class="feature-hint">
      三个问题都选完会自动判定优先级：<b>三问全中 → P0</b>（第一版必做）；<b>中一两项 → P1</b>（验证后再做）；<b>都没中 → P2</b>（不做清单）。
    </div>
  </div>
</template>
